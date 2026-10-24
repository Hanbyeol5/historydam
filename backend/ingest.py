import json
import chromadb

# 1. 'chroma_db'라는 이름의 스마트 정리함 생성
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_or_create_collection(name="history_collection")


def load_and_ingest():
  # 2. history_data.json 파일 상자 열기
  with open("history_api/history_data.json", "r", encoding="utf-8") as f:
    data = json.load(f)

  # 3. 카드를 하나씩 꺼내서 정리함에 넣기
  for index, item in enumerate(data):
    # 기존 안드로이드/FastAPI 연동을 위해 figures가 없다면 기본값 처리
    figures_list = item.get("figures", ["조선시대"])
    figures_str = (
        ",".join(figures_list)
        if isinstance(figures_list, list)
        else figures_list
    )

    # 기존 id가 있으면 쓰고, 없으면 자동으로 생성
    doc_id = item.get("id", f"doc_{index}")

    collection.upsert(
        documents=[f"{item['title']} - {item['content']}"],
        metadatas=[{
            "title": item.get("title", ""),
            "year": item.get("year", 0),
            "figures": figures_str,  # 👈 안드로이드 연동용 기존 키 유지!
            "category": item.get(
                "category", "relic"
            ),  # 👈 우리가 새로 추가한 도장 추가!
            "character_id": item.get("character_id", "chosun_general"),
        }],
        ids=[doc_id],
    )

  print(
      f"🎉 성공! 총 {len(data)}개의 역사 데이터를 안드로이드 호환 형태로"
      " DB에 넣었습니다!"
  )


if __name__ == "__main__":
  load_and_ingest()