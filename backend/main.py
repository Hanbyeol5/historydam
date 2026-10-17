import asyncio
import os
import re  # HTML 태그 정제용 정규식
import xml.etree.ElementTree as ET
from urllib.parse import unquote
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
import google.generativeai as genai
import httpx
from pydantic import BaseModel

load_dotenv()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
PUBLIC_API_KEY = os.getenv("PUBLIC_API_KEY")
KDP_API_KEY = os.getenv("KDP_API_KEY")

if not GEMINI_API_KEY:
  raise ValueError("GEMINI_API_KEY가 .env 파일에 없습니다!")

DECODED_PUBLIC_KEY = (
    unquote(PUBLIC_API_KEY) if PUBLIC_API_KEY else PUBLIC_API_KEY
)

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel("models/gemini-3.8-flash")

app = FastAPI(title="역사담 실시간 다중 API 페르소나 RAG 서버")


class QuestionRequest(BaseModel):
  figureId: str
  question: str


FIGURE_INFO = {
    "sejong": {
        "name": "세종대왕",
        "era": "조선",
        "max_year": 1450,
        "keyword": "세종",
        "persona": (
            "조선의 제4대 왕이다. 백성을 아끼는 자애롭고 위엄 있는 어조(~하노라,"
            " ~이니라)를 쓴다."
        ),
    },
    "yi_sun_sin": {
        "name": "이순신",
        "era": "조선",
        "max_year": 1598,
        "keyword": "이순신",
        "persona": (
            "조선의 수군절도사이다. 장수로서 결연하고 강직한 어조(~하였소,"
            " ~하겠네)를 쓴다."
        ),
    },
    "jang_yeongsil": {
        "name": "장영실",
        "era": "조선",
        "max_year": 1450,
        "keyword": "장영실",
        "persona": (
            "조선의 천민 출신 과학자이다. 신분은 낮았으나 기술에 대한 열정이"
            " 넘치며, 겸손하고 공손한 어조(~하옵니다, ~였습니다)를 쓴다."
        ),
    },
}


def clean_text(text: str) -> str:
  """HTML 태그 (<em>, <cm> 등) 및 노이즈 정제 함수"""
  if not text:
    return ""
  cleaned = re.sub(r"<.*?>", "", text)
  return cleaned.strip()


# --- ⚡ 5대 핵심 공공 API 비동기 호출 및 빈 데이터 검증 함수들 ---


async def fetch_museum(client: httpx.AsyncClient, keyword: str):
  """1. 국립중앙박물관 소장품 API"""
  url = "https://apis.data.go.kr/1371027/openapi/list"
  params = {
      "serviceKey": DECODED_PUBLIC_KEY,
      "pageNo": "1",
      "numOfRows": "5",
      "nameKr": keyword,
  }
  try:
    response = await client.get(url, params=params, timeout=5.0)
    root = ET.fromstring(response.content)
    contexts = []
    for item in root.findall(".//item"):
      item_texts = [
          clean_text(child.text) for child in item if child.text and child.text.strip()
      ]
      if item_texts:
        contexts.append(
            f"[출처: 국립중앙박물관 API] {' '.join(item_texts[:3])}"
        )

    res = contexts[:2]
    if res:
      print(f"🏛️ [박물관 API] 성공: 유효 데이터 {len(res)}건 확보")
    else:
      print(f"⚠️ [박물관 API] 경고: 검색 결과 데이터가 0건입니다.")
    return res
  except Exception as e:
    print(f"❌ [박물관 API 에러]: {e}")
    return []


async def fetch_archives(client: httpx.AsyncClient, keyword: str):
  """2. 국가기록원 나라기록물 검색 API"""
  url = "https://apis.data.go.kr/1741050/openapi/searcharc"
  params = {
      "serviceKey": DECODED_PUBLIC_KEY,
      "pageNo": "1",
      "display": "3",
      "query": keyword,
  }
  try:
    response = await client.get(url, params=params, timeout=5.0)
    root = ET.fromstring(response.content)
    if root.find(".//searchError") is not None:
      err_msg = (
          root.find(".//message").text
          if root.find(".//message") is not None
          else "에러"
      )
      print(f"⚠️ [국가기록원 API] 에러 응답 발생: {err_msg}")
      return []

    contexts = []
    for item in root.findall(".//item"):
      title = (
          clean_text(item.find("title").text)
          if item.find("title") is not None and item.find("title").text
          else "기록물"
      )
      content = (
          clean_text(item.find("content").text)
          if item.find("content") is not None and item.find("content").text
          else ""
      )
      contexts.append(f"[출처: 국가기록원 API] {title}: {content}")

    res = contexts[:2]
    if res:
      print(f"📜 [국가기록원 API] 성공: 유효 데이터 {len(res)}건 확보")
    else:
      print(f"⚠️ [국가기록원 API] 경고: 검색 결과 데이터가 0건입니다.")
    return res
  except Exception as e:
    print(f"❌ [국가기록원 API 에러]: {e}")
    return []


async def fetch_kdp(client: httpx.AsyncClient, keyword: str):
  """3. 한국학자료통합플랫폼(KDP) API"""
  url = "https://kdp.aks.ac.kr/api/search"
  params = {"key": KDP_API_KEY, "keyword": keyword, "page": 1, "size": 3}
  try:
    response = await client.get(url, params=params, timeout=5.0)
    if response.status_code != 200:
      print(f"⚠️ [KDP API] HTTP 상태코드 비정상: {response.status_code}")
      return []

    root = ET.fromstring(response.content)
    contexts = []
    items = (
        root.findall(".//item")
        or root.findall(".//result")
        or root.findall(".//doc")
    )
    for item in items:
      item_texts = [
          clean_text(child.text) for child in item if child.text and child.text.strip()
      ]
      if item_texts:
        contexts.append(
            f"[출처: 한국학자료통합플랫폼(KDP) API] {' '.join(item_texts[:3])}"
        )

    res = contexts[:2]
    if res:
      print(f"📚 [한국학플랫폼 API] 성공: 유효 데이터 {len(res)}건 확보")
    else:
      print(f"⚠️ [한국학플랫폼 API] 경고: 검색 결과 데이터가 0건입니다.")
    return res
  except Exception as e:
    print(f"❌ [한국학플랫폼 API 에러]: {e}")
    return []


async def fetch_dictionary(client: httpx.AsyncClient, keyword: str):
  """4. 국가유산 지식이음 - 전국고교차사전 API (idx=39)"""
  url = "http://portal.nrich.go.kr/kor/openapi.do"
  params = {"idx": "39", "firstIndex": "1", "recordcountperpage": "20"}  # 20건으로 범위 확대
  try:
    response = await client.get(url, params=params, timeout=5.0)
    root = ET.fromstring(response.content)
    contexts = []
    for item in root.findall(".//item"):
      name = (
          clean_text(item.find("NAME").text)
          if item.find("NAME") is not None and item.find("NAME").text
          else ""
      )
      content = (
          clean_text(item.find("CONTENT").text)
          if item.find("CONTENT") is not None and item.find("CONTENT").text
          else ""
      )
      if keyword in name or keyword in content:
        contexts.append(f"[출처: 고교차사전 API] {name}: {content[:100]}")

    res = contexts[:2]
    if res:
      print(f"📖 [고교차사전 API] 성공: 유효 데이터 {len(res)}건 확보")
    else:
      print(f"⚠️ [고교차사전 API] 경고: 검색어 매칭 데이터 0건")
    return res
  except Exception as e:
    print(f"❌ [고교차사전 API 에러]: {e}")
    return []


async def fetch_diary(client: httpx.AsyncClient, keyword: str):
  """5. 국가유산 지식이음 - 조선시대 지도일기 API (idx=205)"""
  url = "http://portal.nrich.go.kr/kor/openapi.do"
  params = {"idx": "205", "firstIndex": "1", "recordcountperpage": "20"}  # 20건으로 범위 확대
  try:
    response = await client.get(url, params=params, timeout=5.0)
    root = ET.fromstring(response.content)
    contexts = []
    for item in root.findall(".//item"):
      title = (
          clean_text(item.find("TITLE_KOR").text)
          if item.find("TITLE_KOR") is not None and item.find("TITLE_KOR").text
          else ""
      )
      content = (
          clean_text(item.find("DCONTENT").text)
          if item.find("DCONTENT") is not None and item.find("DCONTENT").text
          else ""
      )
      if keyword in title or keyword in content or not keyword:
        contexts.append(f"[출처: 조선시대 지도일기 API] {title}: {content[:100]}")

    res = contexts[:2]
    if res:
      print(f"🗺️ [지도일기 API] 성공: 유효 데이터 {len(res)}건 확보")
    else:
      print(f"⚠️ [지도일기 API] 경고: 검색어 매칭 데이터 0건")
    return res
  except Exception as e:
    print(f"❌ [지도일기 API 에러]: {e}")
    return []


# --- 🚀 핵심 RAG API 엔드포인트 ---


@app.post("/v1/chat")
async def ask_history_figure(request: QuestionRequest):
  figure_id = request.figureId
  user_question = request.question

  info = FIGURE_INFO.get(
      figure_id,
      {
          "name": figure_id,
          "era": "역사 속",
          "max_year": 2026,
          "keyword": figure_id,
          "persona": f"역사적 인물 '{figure_id}'이다. 품격 있는 어조로 답하라.",
      },
  )

  figure_name = info["name"]
  figure_era = info["era"]
  max_year = info["max_year"]
  search_keyword = info["keyword"]
  character_persona = info["persona"]

  print("\n" + "=" * 65)
  print(
      f"🚀 [새 요청 수신] 인물: {figure_name} ({figure_era}) | 검색어:"
      f" '{search_keyword}' | 질문: '{user_question}'"
  )
  print("=" * 65)

  # ⚡ 5개 정예 공공 API 실시간 비동기 병렬 호출
  async with httpx.AsyncClient() as client:
    results = await asyncio.gather(
        fetch_museum(client, search_keyword),
        fetch_archives(client, search_keyword),
        fetch_kdp(client, search_keyword),
        fetch_dictionary(client, search_keyword),
        fetch_diary(client, search_keyword),
    )

  all_contexts = [item for sublist in results for item in sublist]
  print("-" * 65)
  print(
      f"📊 [종합 결과] 총 유효 사료 데이터: {len(all_contexts)}건 수집 완료"
  )
  print("=" * 65 + "\n")

  context_text = (
      "\n".join(all_contexts)
      if all_contexts
      else f"'{figure_name}'와 관련된 직접적인 공공 API 기록을 찾지 못했습니다."
  )

  prompt = f"""
너는 {figure_era}시대를 살다 간 역사적 인물 '{figure_name}'이다. 
[상세 신분 및 말투 지침]
{character_persona}

사용자의 질문에 답할 때, 다음 원칙을 반드시 지켜라:
1. 현대인의 말투나 인공지능스러운 말투("안녕하세요", "AI입니다" 등)를 절대 쓰지 말고, 위 [상세 신분 및 말투 지침]에 정의된 인물 고유의 어조를 철저히 지켜라.
2. 아래 제공되는 [실시간 참고 역사 자료]를 적극적으로 활용하여 역사적 사실에 기반해 깊이 있게 답변하라.
3. 자료에 구체적인 내용이 없더라도 인물의 업적과 성향에 맞추어 지혜롭게 답변하되, 역사적 사실을 왜곡하지 말라.
4. 너는 네가 세상을 떠난 {max_year}년 이후에 일어난 미래의 역사나 현대의 일에 대해서는 전혀 알지 못한다. 참고 지식에 없거나 네가 살던 시기 이후의 미래 일을 물어보면 인물의 성격에 맞춰 "내가 살아있을 적에는 들어보지 못한 후세의 일이로다"라는 식으로 페르소나를 유지하며 모른다고 답하라.

[실시간 참고 역사 자료]
{context_text}

[사용자 질문]
{user_question}
"""

  try:
    response = model.generate_content(prompt)
    return {
        "question": user_question,
        "answer": response.text,
        "referenced_data": all_contexts,
    }
  except Exception as e:
    raise HTTPException(status_code=500, detail=str(e))