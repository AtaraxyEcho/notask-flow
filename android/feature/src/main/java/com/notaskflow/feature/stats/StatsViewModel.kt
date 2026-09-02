package com.notaskflow.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notaskflow.domain.model.MemberTaskLoad
import com.notaskflow.domain.model.PersonalNoteTrend
import com.notaskflow.domain.model.PersonalStats
import com.notaskflow.domain.model.RoleCompletion
import com.notaskflow.domain.model.StatsActivity
import com.notaskflow.domain.model.TaskTrend
import com.notaskflow.domain.stats.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val isLoading: Boolean = false,
    val personalStats: PersonalStats? = null,
    val personalNoteTrends: List<PersonalNoteTrend> = emptyList(),
    val trends: List<TaskTrend> = emptyList(),
    val roleCompletions: List<RoleCompletion> = emptyList(),
    val loads: List<MemberTaskLoad> = emptyList(),
    val activities: List<StatsActivity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = mutableUiState

    fun load(spaceId: Long) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val trendsResult = statsRepository.trend(spaceId)
            val roleResult = statsRepository.roleCompletion(spaceId)
            val loadResult = statsRepository.load(spaceId)
            val activityResult = statsRepository.activities(spaceId)
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    trends = trendsResult.getOrDefault(emptyList()),
                    roleCompletions = roleResult.getOrDefault(emptyList()),
                    loads = loadResult.getOrDefault(emptyList()),
                    activities = activityResult.getOrDefault(emptyList()),
                    errorMessage = trendsResult.exceptionOrNull()?.message
                        ?: roleResult.exceptionOrNull()?.message
                        ?: loadResult.exceptionOrNull()?.message
                        ?: activityResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadPersonal() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val statsResult = statsRepository.personal()
            val trendResult = statsRepository.personalNoteTrend()
            mutableUiState.update {
                it.copy(
                    isLoading = false,
                    personalStats = statsResult.getOrNull() ?: it.personalStats,
                    personalNoteTrends = trendResult.getOrDefault(emptyList()),
                    errorMessage = statsResult.exceptionOrNull()?.message
                        ?: trendResult.exceptionOrNull()?.message
                )
            }
        }
    }
}
