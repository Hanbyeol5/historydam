import json
import chromadb

# 1. 'chroma_db'라는 이름의 스마트 정리함 생성
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_or_create_collection(name="history_collection")

def load_and_ingest():
    # 2. 아까 만든 JSON 파일 상자 열기
    with open("history_data.json", "r", encoding="utf-8") as f:
        data = json.load(f)

    # 3. 카드를 하나씩 꺼내서 스티커(메타데이터)를 붙여 정리함에 넣기
    for item in data:
        figures_str = ",".join(item["figures"])  # 인물 이름을 쉼표로 연결
        
        collection.upsert(
            documents=[item["content"]],
            metadatas=[{
                "year": item["year"],
                "figures": figures_str,
                "title": item["title"]
            }],
            ids=[item["id"]]
        )
    print(f"🎉 성공! 총 {len(data)}개의 역사 데이터를 DB 정리함에 잘 넣었습니다!")

if __name__ == "__main__":
    load_and_ingest()