package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import javax.inject.Inject

/** 선택한 국가유산의 상세(설명·시대·이미지)를 조회한다. */
class GetHeritageDetailUseCase @Inject constructor(
    private val heritageRepository: HeritageRepository,
) {
    suspend operator fun invoke(id: String): Result<RecognizedHeritage> =
        heritageRepository.getHeritageDetail(id)
}
