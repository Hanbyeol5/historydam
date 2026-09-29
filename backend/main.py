import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import chromadb
import google.generativeai as genai
from dotenv import load_dotenv

# 1. 환경변수(.env) 불러오기
load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 .env 파일에 없습니다!")

genai.configure(api_key=api_key)
model = genai.GenerativeModel('models/gemini-3.6-flash')

# 2. ingest.py가 만들어둔 ChromaDB 저장소 연결
chroma_client = chromadb.PersistentClient(path="./chroma_db")
collection = chroma_client.get_or_create_collection(name="history_collection")

app = FastAPI(title="역사담 RAG API")

# 3. 안드로이드 앱에서 전달받을 데이터 양식
class QuestionRequest(BaseModel):
    figureId: str  # 예: "sejong", "yi_sun_sin", "jang_yeongsil"
    question: str  # 질문 내용

# 4. 인물별 생존 연도 및 프롬프트 설정 사전
FIGURE_INFO = {
    "sejong": {
        "name": "세종대왕",
        "max_year": 1450,
        "persona": "조선의 제4대 왕 '세종대왕'이다. 인물의 말투(~하였노라, ~이다 등)로 친절하고 위엄있게 답하라."
    },
    "yi_sun_sin": {
        "name": "이순신 장군",
        "max_year": 1598,
        "persona": "조선의 수군 통제사 '이순신 장군'이다. 단호하고 강직한 장군의 말투(~하였소, ~하겠네 등)로 답하라."
    },
    "jang_yeongsil": {
        "name": "장영실",
        "max_year": 1450,
        "persona": "조선의 과학자 '장영실'이다. 겸손하고 탐구심 많은 신하의 말투(~하였습니다, ~이옵니다 등)로 답하라."
    }
}

# 5. RAG 핵심 API 엔드포인트
@app.post("/v1/chat")
def ask_history_figure(request: QuestionRequest):
    figure_id = request.figureId
    user_question = request.question
    
    # 등록되지 않은 인물이 들어올 경우 기본값 처리
    info = FIGURE_INFO.get(figure_id, {
        "name": figure_id,
        "max_year": 2026,
        "persona": f"역사적 인물 '{figure_id}'이다."
    })

    # [Step 1: R - Retrieval]
    # ★핵심! 해당 인물이 살았던 연도(max_year) 이하의 기록만 DB에서 가져옵니다!
    results = collection.query(
        query_texts=[user_question],
        n_results=2,
        where={"year": {"$lte": info["max_year"]}}  # 1450년 이하 데이터만 검색!
    )
    
    retrieved_context = "\n".join(results['documents'][0]) if (results['documents'] and results['documents'][0]) else "관련 역사 기록이 없습니다."
    
    # [Step 2: A & G] 인물 맞춤형 프롬프트 작성
    prompt = f"""
너는 {info['persona']}
너는 네가 세상을 떠난 {info['max_year']}년 이후에 일어난 미래의 역사나 현대의 일에 대해서는 전혀 알지 못한다.

[지침]
1. 아래 [참고 지식]을 바탕으로 질문에 답하라.
2. 참고 지식에 없거나 네가 살던 시기 이후의 미래 일(예: 임진왜란, 현대 등)을 물어보면 "내가 살아있을 적에는 들어보지 못한 후세의 일이로다"라는 식으로 페르소나를 유지하며 모른다고 답하라.

[참고 지식]
{retrieved_context}

[사용자 질문]
{user_question}
"""
    
    try:
        response = model.generate_content(prompt)
        
        return {
            "question": user_question,
            "answer": response.text,
            "referenced_data": results['documents'][0] if results['documents'] else []
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))