"""
backend/ 의 의도 분류 모델(vectorizer.pkl + intent_model.pkl)을 앱이 읽는 JSON 으로 옮긴다.

  python scripts/export_intent_model.py

- core/backend/src/main/assets/intent_model.json  : 앱이 쓰는 모델 값
- core/backend/src/test/resources/intent_cases.json : 파이썬 예측값 (앱 단위 테스트가 같은 값을 내는지 비교)

backend/train_intent.py 로 모델을 다시 학습했으면 이 스크립트를 다시 돌린다.
"""
import json
import pickle
import warnings
from pathlib import Path

warnings.filterwarnings("ignore")
ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "backend"

vectorizer = pickle.loads((BACKEND / "vectorizer.pkl").read_bytes())
model = pickle.loads((BACKEND / "intent_model.pkl").read_bytes())

params = vectorizer.get_params()
assert params["analyzer"] == "word" and params["ngram_range"] == (1, 1), params
assert params["lowercase"] and params["norm"] == "l2" and not params["sublinear_tf"], params
assert params["token_pattern"] == r"(?u)\b\w\w+\b", params

out = {
    "_comment": (
        "backend/vectorizer.pkl + intent_model.pkl (TfidfVectorizer 기본값 + LogisticRegression) 을 앱용으로 옮긴 값."
        " backend/train_intent.py 로 다시 학습하면 scripts/export_intent_model.py 로 다시 뽑는다."
    ),
    "vocabulary": {k: int(i) for k, i in sorted(vectorizer.vocabulary_.items(), key=lambda kv: kv[1])},
    "idf": [float(x) for x in vectorizer.idf_],
    "coef": [float(x) for x in model.coef_[0]],
    "intercept": float(model.intercept_[0]),
}
asset = ROOT / "core/backend/src/main/assets/intent_model.json"
asset.parent.mkdir(parents=True, exist_ok=True)
asset.write_text(json.dumps(out, ensure_ascii=False, indent=1), encoding="utf-8")

CASES = [
    "안녕하세요", "hello", "반가워 누구야?", "오늘 날씨 어때", "밥 먹었어?",
    "화성은 왜 지으셨습니까?", "훈민정음은 언제 만들었나요", "거북선에 대해 알려줘",
    "측우기를 만든 이유는", "명량해전 이야기 해주세요", "한글 창제 과정이 궁금해",
    "Hello WORLD 세종 업적", "임진왜란 때 무엇을 하셨나요?",
]
X = vectorizer.transform(CASES)
cases = [
    {"text": t, "label": int(p), "score": float(s)}
    for t, p, s in zip(CASES, model.predict(X), model.decision_function(X))
]
test_res = ROOT / "core/backend/src/test/resources/intent_cases.json"
test_res.parent.mkdir(parents=True, exist_ok=True)
test_res.write_text(json.dumps(cases, ensure_ascii=False, indent=1), encoding="utf-8")
print(f"vocabulary {len(out['vocabulary'])} → {asset}")
for c in cases:
    print(c["label"], round(c["score"], 4), c["text"])
