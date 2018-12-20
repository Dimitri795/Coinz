package com.example.dimit.coinz


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendChatMessageTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")

    private var db = FirebaseFirestore.getInstance()
    private var mAuth = FirebaseAuth.getInstance()

    @Before
    fun init(){
        // clear the chat history before testing
        db.collection("Chat").document("Message").delete()
        Log.d("@Before","Cleared Chat!")
    }

    @Test
    fun sendChatMessageTest() {
        // Signs in the test admin, sends hello to the chat and asserts that the chat displays hello

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val appCompatEditText2 = onView(
                allOf(withId(R.id.fieldEmail),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatEditText2.perform(replaceText("admin@example.com"), closeSoftKeyboard())

        val appCompatEditText5 = onView(
                allOf(withId(R.id.fieldPassword),
                        childAtPosition(
                                allOf(withId(R.id.emailPasswordFields),
                                        childAtPosition(
                                                withId(R.id.loginView),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText5.perform(replaceText("adminexample"), closeSoftKeyboard())

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

        Thread.sleep(7000)

        val floatingActionButton = onView(
                allOf(withId(R.id.fab3),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.support.design.widget.CoordinatorLayout")),
                                        2),
                                9),
                        isDisplayed()))
        floatingActionButton.perform(click())

        val appCompatEditText7 = onView(
                allOf(withId(R.id.text_field),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatEditText7.perform(replaceText("hello"), closeSoftKeyboard())

        val appCompatButton2 = onView(
                allOf(withId(R.id.sendMessageButton), withText("Send Message"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()))
        appCompatButton2.perform(click())

        onView(allOf(withId(R.id.incoming_message)))
                .check(matches(withText("admin: hello\n")))

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
    @After
    fun signOut(){
        // sign out after s tests can run in any order since they all start assuming the app is in loginActivity
        mAuth.signOut()
    }
}
