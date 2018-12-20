package com.example.dimit.coinz


import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SignInSignOutTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    @Test
    fun signInSignOutTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val appCompatEditText6 = onView(
                allOf(withId(R.id.fieldEmail),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText6.perform(replaceText("admin@example.com"))

        val appCompatEditText7 = onView(
                allOf(withId(R.id.fieldEmail), withText("admin@example.com"),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText7.perform(closeSoftKeyboard())

        val appCompatEditText9 = onView(
                allOf(withId(R.id.fieldPassword),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText9.perform(replaceText("adminexample"))

        val appCompatEditText10 = onView(
                allOf(withId(R.id.fieldPassword), withText("adminexample"),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText10.perform(closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.emailSignInButton), withText("sign in"),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordButtons),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                1)),
                                0),
                        isDisplayed()))
        appCompatButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val viewGroup = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(android.R.id.content),
                                0),
                        2),
                        isDisplayed()))
        viewGroup.check(matches(isDisplayed()))

        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

        val appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Sign out"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()))
        appCompatTextView.perform(click())

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        Thread.sleep(7000)

        val button = onView(
                allOf(withId(R.id.emailSignInButton),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordButtons),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                1)),
                                0),
                        isDisplayed()))
        button.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
