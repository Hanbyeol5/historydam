# 인물 에셋 / 저작권 정책

CLAUDE.md §12 를 반드시 준수합니다.

- 초상은 **(1) 퍼블릭 도메인 진본 초상화, (2) 정식 라이선스 에셋, (3) 자체 제작 일러스트** 만 사용.
- **현대 표준영정·지폐 도상 등 저작권 있는 이미지의 무단 커밋 금지.**
- 각 인물 디렉토리에 라이선스 메타데이터를 함께 둡니다:

```
assets/figures/<figureId>/
  portrait.png        # 라이선스 확인된 초상
  cutout.png          # AR 용 배경제거 PNG (원본 라이선스 확인분에서만 생성)
  license.json        # { "source": "...", "rights": "...", "usage": "..." }
```

> 현재 시드 데이터(`core:data` 의 `FakeFigureCatalog`)는 저작권 이슈를 피하기 위해
> 초상 URL 을 비워두고, 앱은 인물명 첫 글자 플레이스홀더(MedallionPortrait)를 표시합니다.
