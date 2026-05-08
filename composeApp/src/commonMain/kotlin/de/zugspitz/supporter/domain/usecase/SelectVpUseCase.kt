package de.zugspitz.supporter.domain.usecase

class SelectVpUseCase {
    operator fun invoke(index: Int, lastIndex: Int): Int = index.coerceIn(0, lastIndex)
}
