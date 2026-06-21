package com.example.data

import java.util.Calendar
import java.util.TimeZone

data class DailyGuest(
    val id: Int,
    val name: String,
    val story: String,
    val relationshipType: String, // "Family", "Friend", "Someone Special", "Myself"
    val preferredFlavorId: String,
    val preferredBaseId: String,
    val recurringDialogue: String,
    val freeToppingRotateId: String,
    val freeToppingName: String
)

object GuestManager {
    val guests = listOf(
        DailyGuest(
            id = 0,
            name = "Mira",
            story = "Mira hasn't seen her sister in 3 long years — help her make a warm, welcoming slice.",
            relationshipType = "Family",
            preferredFlavorId = "strawberry",
            preferredBaseId = "vanilla_sponge",
            recurringDialogue = "My sister absolutely adored the slice we made! Let's decorate another sweet strawberry treat today.",
            freeToppingRotateId = "mint_leaf",
            freeToppingName = "Fresh Mint Leaf"
        ),
        DailyGuest(
            id = 1,
            name = "Arthur",
            story = "Arthur is celebrating his 50th golden wedding anniversary with Eleanor. He wants a warm, honeyed treat.",
            relationshipType = "Someone Special",
            preferredFlavorId = "orange",
            preferredBaseId = "carrot_cake",
            recurringDialogue = "Eleanor still blushes thinking about our anniversary slice. Let's make today's creation just as magical.",
            freeToppingRotateId = "gold_flakes",
            freeToppingName = "Golden Sugar Flakes"
        ),
        DailyGuest(
            id = 2,
            name = "Chloe & Toby",
            story = "Toby just got an exciting job promotion. Chloe wants to celebrate with a rich celebration slice.",
            relationshipType = "Friend",
            preferredFlavorId = "chocolate",
            preferredBaseId = "chocolate_fudge",
            recurringDialogue = "Toby is working hard at his new job, let's surprise him! Chocolate is his ultimate favorite.",
            freeToppingRotateId = "star_sprinkles",
            freeToppingName = "Sparkling Stars"
        ),
        DailyGuest(
            id = 3,
            name = "Little Leo",
            story = "Little Leo saved up his chore money to buy a slice for his Grandmother's 80th birthday. Help him make it bright!",
            relationshipType = "Family",
            preferredFlavorId = "vanilla",
            preferredBaseId = "vanilla_sponge",
            recurringDialogue = "Grandma saved the little polaroid in her bedroom! Can we make something super colorful with berries today?",
            freeToppingRotateId = "fresh_raspberries",
            freeToppingName = "Plump Raspberries"
        ),
        DailyGuest(
            id = 4,
            name = "Sienna",
            story = "Sienna is re-connecting with her old college roommate after moving back to town. They used to share citrus snacks.",
            relationshipType = "Friend",
            preferredFlavorId = "orange",
            preferredBaseId = "lemon_sponge",
            recurringDialogue = "It feels like we never spent a day apart! Let's decorate a happy citrus slice together.",
            freeToppingRotateId = "candied_orange",
            freeToppingName = "Candied Citrus Wheel"
        ),
        DailyGuest(
            id = 5,
            name = "Julian",
            story = "Julian finally completed the very last chapter of his epic fantasy novel. He is treating himself to a sweet milestone card.",
            relationshipType = "Myself",
            preferredFlavorId = "vanilla",
            preferredBaseId = "red_velvet",
            recurringDialogue = "Sitting at my favorite window table! Let's celebrate our creative milestones together.",
            freeToppingRotateId = "rosemary_sprig",
            freeToppingName = "Sweet Rosemary"
        ),
        DailyGuest(
            id = 6,
            name = "Nora & Maya",
            story = "Nora is welcoming Maya home for summer break after her busy first semester. She wants a cheerful pink glaze.",
            relationshipType = "Family",
            preferredFlavorId = "strawberry",
            preferredBaseId = "vanilla_sponge",
            recurringDialogue = "Maya is finally resting! Let's decorate a delightful strawberry welcome-home cake.",
            freeToppingRotateId = "white_chocolate_shavings",
            freeToppingName = "White Chocolate Shakes"
        ),
        DailyGuest(
            id = 7,
            name = "Felix",
            story = "Felix is meeting his childhood mentor to thank them for guidance over the years with a comforting cake.",
            relationshipType = "Someone Special",
            preferredFlavorId = "chocolate",
            preferredBaseId = "carrot_cake",
            recurringDialogue = "My mentor appreciated the thought so much! Let's bake another delicious, comforting slice.",
            freeToppingRotateId = "pecan_crumb",
            freeToppingName = "Toasted Pecans"
        )
    )

    fun getGuestForDay(timestamp: Long): DailyGuest {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = timestamp
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % guests.size
        return guests[index]
    }

    // Get number of milliseconds remaining until the next calendar day
    fun getMillisUntilTomorrow(timestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val diff = calendar.timeInMillis - timestamp
        return if (diff > 0) diff else 86400000L
    }

    val tutorialGuest = DailyGuest(
        id = -1,
        name = "Clover",
        story = "Hey there, I am Clover, the owner! Let me guide you through decorating your first slice to learn our tools.",
        relationshipType = "Myself",
        preferredFlavorId = "vanilla",
        preferredBaseId = "vanilla_sponge",
        recurringDialogue = "Welcome back, talent!",
        freeToppingRotateId = "rainbow_sprinkles",
        freeToppingName = "Rainbow Sprinkles"
    )
}
