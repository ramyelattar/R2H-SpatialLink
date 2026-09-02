package com.r2h.spatiallink.nearby

object NearbyFieldStateMapper {
    fun map(state: NearbyUiState): NearbyPresentation = state.toPresentation()
}
