import chromadb

# 1. 아까 그 스마트 정리함(chroma_db)에 접속하기
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_or_create_collection(name="history_collection")


def test_search():
  # 2. 챗봇에게 물어볼 예시 질문 던지기 (예: "숭례문")
  query = "숭례문"
  print(f"🔍 질문: '{query}'(이)와 관련된 역사 지식을 검색하는 중...\n")

  # 3. ChromaDB에게 의미가 가장 비슷한 데이터 3개만 뽑아달라고 요청하기
  results = collection.query(query_texts=[query], n_results=3)

  # 4. 결과 예쁘게 출력해보기
  for i, (doc, metadata) in enumerate(
      zip(results["documents"][0], results["metadatas"][0])
  ):
    print(f"--- [검색 결과 {i+1}] ---")
    print(f"📄 본문: {doc}")
    print(
        f"🏷️ 스티커(메타데이터): 연도={metadata.get('year')}, 키워드="
        f"{metadata.get('figures')}, 카테고리={metadata.get('category')}\n"
    )


if __name__ == "__main__":
  test_search()