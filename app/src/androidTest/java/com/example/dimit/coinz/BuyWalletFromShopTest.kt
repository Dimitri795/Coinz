package com.example.dimit.coinz


import android.content.Context
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView.ViewHolder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BuyWalletFromShopTest {

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
    private var gold = 0

    @Before
    fun init(){
        // gets the test admin current gold value and adds 10,000 to it to afford the item buying
        // clears the shared preferences for items so the default items are used

        val docRef = db.collection("Users").document("ZyIKmgPEZ9QTnpcehtF0EL5upaH3")
        docRef.get().addOnSuccessListener {
            gold = (it.data!!["GoldCount"] as Long).toInt()
        }

        val settings = mActivityTestRule.activity.getSharedPreferences("MyPrefsFileZyIKmgPEZ9QTnpcehtF0EL5upaH3", Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("AvailableItemList","")
        Log.d("@Before","Item list set to default")
        editor.apply()
    }

    @Test
    fun buyWalletFromShopTest() {
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
                allOf(withId(R.id.fab2),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                4),
                        isDisplayed()))
        floatingActionButton.perform(click())

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val recyclerView2 = onView(
                allOf(withId(R.id.Shop),
                        childAtPosition(
                                withId(R.id.frameLayout),
                                0)))
        recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val textView = onView(
                allOf(withId(R.id.item_detail),
                        childAtPosition(
                                allOf(withId(R.id.item_detail_container),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                                1)),
                                0),
                        isDisplayed()))
        textView.check(matches(withText("Did you already deposit your 25 Coin limit only to find the coin of your dreams? " +
                "Did trading it break your heart and made you wish there was some way to deposit just one more coin? " +
                "\nWELL YOU\'RE IN LUCK FRIEND! With this new wallet with ONE extra pouch you can deposit a whopping 26 coins! " +
                "\n \nPrice: 10000 gold")))

        val appCompatButton2 = onView(
                allOf(withId(R.id.buyItemButton), withText("Buy"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton2.perform(click())

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val recyclerView3 = onView(
                allOf(withId(R.id.Shop),
                        childAtPosition(
                                withId(R.id.frameLayout),
                                0)))
        recyclerView3.perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        // Added a sleep statement to match the app's execution delay.
        Thread.sleep(7000)

        val textView2 = onView(
                allOf(withId(R.id.item_detail),
                        childAtPosition(
                                allOf(withId(R.id.item_detail_container),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                                1)),
                                0),
                        isDisplayed()))
        textView2.check(matches(withText("Has the spirit of capitalism sunk its claws into you and 26 coins is no longer enough? " +
                "Would you rather hoard your remaining coins than trade them to Barry for pocket change AGAIN? " +
                "\nTHEN THIS IS THE WALLET FOR YOU! Now packing TWO extra pouches you can deposit 27 coins easily! " +
                "\n \nPrice: 15000 gold")))
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
        // Sign out test admin because tests expect the app to be in login activity and are run in random order
        //resets gold to the value at the start of the test
        mAuth.signOut()
        val goldcount = HashMap<String,Any>()
        goldcount["GoldCount"] = gold
        db.collection("Users").document("ZyIKmgPEZ9QTnpcehtF0EL5upaH3").set(goldcount, SetOptions.merge())
    }
}
