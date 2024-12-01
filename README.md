# VaChart - Indian Railway Vacancy Seat Checker

## About The App
VaChart is a modern Android application that helps travelers find vacant seats in Indian Railway trains after the chart preparation. Built with the latest Android technologies, it provides a seamless and intuitive experience for checking train vacancies.

## Features
- 🔍 **Real-time Vacancy Check**: Instantly view available seats after chart preparation
- 🚂 **Detailed Coach Information**: See vacancy status for each coach type
- 📱 **Modern UI**: Built with Jetpack Compose and Material 3 design
- 🌓 **Theme Options**: Support for both light and dark themes
- ⚡ **Efficient Performance**: Utilizes MVVM architecture for smooth operation

## App Showcase

### Light Theme Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="/screenshots/home.png" alt="Home Screen" width="200"/><br>
        <sub><b>Home Screen</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/traindetail.png" alt="Train Details" width="200"/><br>
        <sub><b>Train Details</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/coach.png" alt="Coach View" width="200"/><br>
        <sub><b>Coach Selection</b></sub>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src="/screenshots/coachbottom.png" alt="Coach Bottom Sheet" width="200"/><br>
        <sub><b>Coach Information</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/vacantberth.png" alt="Vacant Berth" width="200"/><br>
        <sub><b>Vacant Berth Display</b></sub>
      </td>
      <td></td>
    </tr>
  </table>
</div>

### Dark Theme Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="/screenshots/home_dark.png" alt="Home Screen Dark" width="200"/><br>
        <sub><b>Home Screen</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/traindetail_dark.png" alt="Train Details Dark" width="200"/><br>
        <sub><b>Train Details</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/coach_dark.png" alt="Coach View Dark" width="200"/><br>
        <sub><b>Coach Selection</b></sub>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src="/screenshots/coachbottom_dark.png" alt="Coach Bottom Sheet Dark" width="200"/><br>
        <sub><b>Coach Information</b></sub>
      </td>
      <td align="center">
        <img src="/screenshots/vacantberth_dark.png" alt="Vacant Berth Dark" width="200"/><br>
        <sub><b>Vacant Berth Display</b></sub>
      </td>
      <td></td>
    </tr>
  </table>
</div>

## Technical Architecture
### Built With
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: Modern toolkit for building native UI
- **[Material 3](https://m3.material.io/)**: Latest Material Design implementation
- **[Hilt](https://dagger.dev/hilt/)**: For dependency injection
- **[Kotlin Flows](https://kotlinlang.org/docs/flow.html)**: For reactive data management
- **MVVM Architecture**: For clean separation of concerns

### Architecture Components
```kotlin
@HiltAndroidApp
class VaCartApplication : Application()

@HiltViewModel
class TrainViewModel @Inject constructor(
    private val trainRepository: TrainRepository
) : ViewModel() {
    private val _vacancyState = MutableStateFlow<VacancyState>(VacancyState.Loading)
    val vacancyState: StateFlow<VacancyState> = _vacancyState.asStateFlow()
    
    fun getVacancy(trainNumber: String) {
        viewModelScope.launch {
            trainRepository.getVacancy(trainNumber)
                .collect { vacancy ->
                    _vacancyState.value = VacancyState.Success(vacancy)
                }
        }
    }
}
```

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11
- Android SDK 21 or above

### Installation
1. Clone the repository
```bash
git clone https://github.com/CHAHATMB/VaChart.git
```

2. Open in Android Studio

3. Build and run the app

## Roadmap
- [ ] Add offline support
- [ ] Implement notifications for chart preparation
- [ ] Add in train chat feature
- [ ] Support for multiple languages

## License
This project is licensed under the MIT License
## Acknowledgments
- Indian Railways for providing the data
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design](https://material.io/)
- 
