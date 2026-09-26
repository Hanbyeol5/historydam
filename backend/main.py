import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import chromadb
import google.generativeai as genai
from dotenv import load_dotenv

# 1. 환경변수(.env)에서 API 키 불러오기
load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 .env 파일에 없습니다!")

# 2. Gemini AI 설정
genai.configure(api_key=api_key)
model = genai.GenerativeModel('models/gemini-3.6-flash')

# 3. 내장 저장소 (ChromaDB Vector DB) 설정
# 데이터를 메모리에 보관하는 쉬운 도서관을 하나 만듭니다.
chroma_client = chromadb.Client()
collection = chroma_client.create_collection(name="history_sejong")

# 4. sejong.txt 파일을 읽어서 Vector DB에 집어넣기 (최초 1회 저장)
with open("sejong.txt", "r", encoding="utf-8") as f:
    text_data = f.read()

# 문장/단락 단위로 쪼개기
lines = [line.strip() for line in text_data.split("\n") if line.strip()]

# ChromaDB에 저장 (자동으로 글자를 숫자로 변환해서 기억합니다)
for index, line in enumerate(lines):
    collection.add(
        documents=[line],
        ids=[f"id_{index}"]
    )

print(f"✅ 총 {len(lines)}개의 역사 데이터 문장이 저장되었습니다!")

# 5. FastAPI 서버 생성
app = FastAPI(title="역사담 RAG API")

# 앱에서 전달받을 데이터 양식 정의
class QuestionRequest(BaseModel):
    question: str

# 6. RAG 핵심 API 엔드포인트 (안드로이드 앱이 질문을 보낼 주소)
@app.post("/chat")
def ask_sejong(request: QuestionRequest):
    user_question = request.question
    
    # [Step 1: R - Retrieval] 질문과 관련된 역사 기록 DB에서 찾아오기
    results = collection.query(
        query_texts=[user_question],
        n_results=2 # 가장 관련 깊은 문장 2개 가져오기
    )
    
    # 찾아온 관련 문장들을 하나로 합치기
    retrieved_context = "\n".join(results['documents'][0]) if results['documents'] else ""
    
    # [Step 2: A - Augment & G - Generate] 프롬프트 구성 및 Gemini 답변 생성
    prompt = f"""
너는 조선의 제4대 왕 '세종대왕'이다. 인물의 말투(~하였노라, ~이다 등)로 친절하게 답하라.
아래 참고 지식을 바탕으로 사용자의 질문에 답하라. 참고 지식에 없는 내용은 모른다고 솔직히 말하라.

[참고 지식]
{retrieved_context}

[사용자 질문]
{user_question}
"""
    
    try:
        response = model.generate_content(prompt)
        
        # 앱에게 전송할 결과값 리턴
        return {
            "question": user_question,
            "answer": response.text,
            "referenced_data": results['documents'][0] # 어떤 문장을 참고했는지 확인용
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))