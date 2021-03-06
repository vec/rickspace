package com.app.juawcevada.rickspace.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.juawcevada.rickspace.R
import com.app.juawcevada.rickspace.R.id.error_icon
import com.app.juawcevada.rickspace.R.id.error_retry_button
import com.app.juawcevada.rickspace.R.id.error_text_title
import com.app.juawcevada.rickspace.R.id.list
import com.app.juawcevada.rickspace.R.id.snackbar_text
import com.app.juawcevada.rickspace.R.id.spin_kit
import com.app.juawcevada.rickspace.R.id.swipe_to_refresh
import com.app.juawcevada.rickspace.event.Event
import com.app.juawcevada.rickspace.testing.SingleFragmentActivity
import com.app.juawcevada.rickspace.ui.characterlist.CharacterListFragment
import com.app.juawcevada.rickspace.ui.characterlist.CharacterListFragmentDirections
import com.app.juawcevada.rickspace.ui.characterlist.CharacterListNavigationActions
import com.app.juawcevada.rickspace.ui.characterlist.CharacterListViewModel
import com.app.juawcevada.rickspace.ui.characterlist.CharacterListViewState
import com.app.juawcevada.rickspace.ui.shared.FragmentBindingAdapters
import com.app.juawcevada.rickspace.ui.shared.FragmentDataBindingComponent
import com.app.juawcevada.rickspace.ui.shared.SnackbarMessage
import com.app.juawcevada.rickspace.util.TestDataSourceFactory
import com.app.juawcevada.rickspace.util.all
import com.app.juawcevada.rickspace.util.builder.character
import com.app.juawcevada.rickspace.util.checkThatMatches
import com.app.juawcevada.rickspace.util.createTestFactory
import com.app.juawcevada.rickspace.util.onRecyclerViewPosition
import com.app.juawcevada.rickspace.util.perform
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CharacterListFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var characterListViewModel: CharacterListViewModel
    private lateinit var navigationAction: MutableLiveData<Event<CharacterListNavigationActions>>
    private lateinit var errorMessage: MutableLiveData<Event<SnackbarMessage>>
    private lateinit var viewState: MutableLiveData<CharacterListViewState>
    private lateinit var fragment: CharacterListFragmentMockNavigation
    private lateinit var activityScenario: ActivityScenario<SingleFragmentActivity>

    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(SingleFragmentActivity::class.java)

        navigationAction = MutableLiveData()
        errorMessage = MutableLiveData()
        viewState = MutableLiveData()
        fragment = CharacterListFragmentMockNavigation()

        characterListViewModel = mock {
            on { viewState } doReturn this@CharacterListFragmentTest.viewState
            on { errorMessage } doReturn this@CharacterListFragmentTest.errorMessage
            on { navigationAction } doReturn this@CharacterListFragmentTest.navigationAction
        }

        fragment.viewModelFactory = characterListViewModel.createTestFactory()

        fragment.fragmentDataBindingComponent = object : FragmentDataBindingComponent(fragment) {
            override fun getFragmentBindingAdapters() = mock<FragmentBindingAdapters>()
        }

    }

    @Test
    fun loading() {
        viewState.value = CharacterListViewState(isLoading = true)
        startFragment()

        spin_kit checkThatMatches isDisplayed()
    }

    @Test
    fun error() {
        val errorMessage = "Oops universe not found!"
        viewState.value = CharacterListViewState(errorMessage = errorMessage)
        startFragment()

        error_text_title checkThatMatches withText(errorMessage)
        error_icon checkThatMatches isDisplayed()
        error_retry_button checkThatMatches isDisplayed()
    }


    @Test
    fun showCharacterList() {
        val charactersList = mutableListOf(
                character {
                    name { "Rick" }
                    location {
                        name { "Earth" }
                    }
                    origin {
                        name { "Another Earth" }
                    }
                    species { "Human" }
                },
                character {
                    name { "Morty" }
                    location {
                        name { "Earth" }
                    }
                    origin {
                        name { "Another Earth" }
                    }
                    species { "Human" }
                }
        )
        val characterPagedList = TestDataSourceFactory(charactersList).buildPagedList()
        viewState.value = CharacterListViewState(charactersList = characterPagedList)
        startFragment()

        list onRecyclerViewPosition 0 checkThatMatches all {
            matcher { hasDescendant(withText("Rick")) }
            matcher { hasDescendant(withText("Earth")) }
            matcher { hasDescendant(withText("Another Earth")) }
            matcher { hasDescendant(withText("Human")) }
        }

        list onRecyclerViewPosition 1 checkThatMatches all {
            matcher { hasDescendant(withText("Morty")) }
            matcher { hasDescendant(withText("Earth")) }
            matcher { hasDescendant(withText("Another Earth")) }
            matcher { hasDescendant(withText("Human")) }
        }
    }


    @Test
    fun navigateToDetail() {
        val charactersList = mutableListOf(
                character {
                    id { 1 }
                }
        )
        val characterPagedList = TestDataSourceFactory(charactersList).buildPagedList()
        viewState.value = CharacterListViewState(charactersList = characterPagedList)
        navigationAction.postValue(Event(CharacterListNavigationActions.OpenCharacterDetail(1)))
        startFragment()

        list onRecyclerViewPosition 0 perform click()

        verify(characterListViewModel).openCharacter(1)

        val navigationAction =
                CharacterListFragmentDirections
                        .actionCharacterListFragmentToCharacterDetailFragment(1)
        verify(fragment.navController).navigate(navigationAction)
    }

    @Test
    fun refresh() {
        val charactersList = mutableListOf(
                character {}
        )

        val characterPagedList = TestDataSourceFactory(charactersList).buildPagedList()
        viewState.value = CharacterListViewState(charactersList = characterPagedList)
        startFragment()

        swipe_to_refresh perform swipeDown()

        verify(characterListViewModel).refresh()
    }


    @Test
    fun retry() {
        val errorMessage = "Meeseeks are asleep now!"
        viewState.value = CharacterListViewState(errorMessage = errorMessage)
        startFragment()

        error_retry_button perform click()

        verify(characterListViewModel).retry()
    }

    @Test
    fun errorToast() {
        val charactersList = mutableListOf(
                character {}
        )
        startFragment()

        val characterPagedList = TestDataSourceFactory(charactersList).buildPagedList()
        viewState.value = CharacterListViewState(charactersList = characterPagedList)
        errorMessage.value = Event(SnackbarMessage(R.string.default_error_message))

        snackbar_text checkThatMatches withText(R.string.default_error_message)
    }

    private fun startFragment() {
        activityScenario.onActivity {
            it.replaceFragment(fragment)
        }
    }

    class CharacterListFragmentMockNavigation : CharacterListFragment() {
        val navController = mock<NavController>()
        override fun navController() = navController
    }
}