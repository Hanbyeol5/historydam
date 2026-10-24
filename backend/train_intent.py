import numpy as np
import pickle
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split

# 1. 공부할 예제 데이터 (질문과 정답 라벨)
# 라벨 0: 단순 인사/일상 대화 (API 안 부름)
# 라벨 1: 역사 사료 질의 (5대 공공 API RAG 호출)
texts = [
    # 잡담 (라벨 0)
    "안녕?",
    "너 누구야?",
    "반가워",
    "오늘 날씨 어때?",
    "심심한데 얘기나 하자",
    "하이",
    "고마워",
    "안녕 세종대왕",
    "잘 지내니?",
    "뭐 하고 있어?",
    # 역사 질문 (라벨 1)
    "세종대왕 업적 알려줘",
    "이순신 장군 거북선은 몇 척이었어?",
    "훈민정음 창제 이유가 뭐야?",
    "장영실이 만든 자격루 설명해줘",
    "조선시대 왕들의 일기를 보여줘",
    "임진왜란 때 이순신 상황 어땠어?",
    "측우기는 누가 발명했지?",
    "한글은 언제 만들어졌어?",
    "세종대왕의 업적을 요약해줘",
    "이순신 장군의 탄생일은?",
]
labels = [
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,  # 잡담 10개
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,  # 역사 질문 10개
]

print("🤖 [AI 학습 시작] 의도 분류 모델(Logistic Regression) 학습을 진행합니다...")

# 2. 문장을 컴퓨터가 이해할 수 있는 숫자 특징(벡터)으로 변환
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(texts)
y = np.array(labels)

# 3. 데이터를 공부용(80%)과 시험용(20%)으로 나누기
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# 4. 머신러닝 모델 학습시키기
model = LogisticRegression()
model.fit(X_train, y_train)

# 5. 시험(평가) 치기 및 성적표(정확도) 뽑기
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)

print("\n" + "=" * 40)
print(f"🎉 [AI 학습 및 평가 완료!] 모델 테스트 정확도(Accuracy): {accuracy * 100:.1f}%")
print("=" * 40)
print("\n[상세 평가 보고서]")
print(classification_report(y_test, y_pred, zero_division=0))

# 모델과 텍스트 변환기를 파일로 저장 (pickle 활용)
with open("intent_model.pkl", "wb") as f:
  pickle.dump(model, f)

with open("vectorizer.pkl", "wb") as f:
  pickle.dump(vectorizer, f)

print("💾 [저장 완료] 의도 분류 모델과 벡터라이저가 .pkl 파일로 저장되었습니다!")