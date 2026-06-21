package com.example.data

import androidx.compose.ui.graphics.Color

data class CakeBase(
    val id: String,
    val name: String,
    val baseColor: Color,
    val fillingColor: Color,
    val description: String,
    val isLimited: Boolean = false,
    val limitedCollectionName: String? = null
)

data class FrostingFlavor(
    val id: String,
    val name: String,
    val color: Color,
    val sparkle: Boolean = false,
    val isLimited: Boolean = false,
    val limitedCollectionName: String? = null
)

data class Spatula(
    val id: Int,
    val name: String,
    val bladeDesc: String,
    val strokeWidth: Float,
    val alpha: Float,
    val description: String
)

data class Topping(
    val id: String,
    val name: String,
    val isLimited: Boolean = false,
    val limitedCollectionName: String? = null
)

object DecorationOptions {
    val cakeBases = listOf(
        CakeBase(
            id = "vanilla_sponge",
            name = "Vanilla Golden Sponge",
            baseColor = Color(0xFFF9E4B7),
            fillingColor = Color(0xFFFFB7B2),
            description = "A light, fluffy traditional vanilla sponge with strawberry custard filling."
        ),
        CakeBase(
            id = "chocolate_fudge",
            name = "Rich Chocolate Fudge",
            baseColor = Color(0xFF4E3629),
            fillingColor = Color(0xFF33221A),
            description = "Deep, velvety chocolate crumb with a decadent mocha filling."
        ),
        CakeBase(
            id = "red_velvet",
            name = "Royal Red Velvet",
            baseColor = Color(0xFF8B2635),
            fillingColor = Color(0xFFF7F4EB),
            description = "A beautiful rich crimson sponge with dense vanilla bean cream filling."
        ),
        CakeBase(
            id = "lemon_sponge",
            name = "Zesty Lemon Cloud",
            baseColor = Color(0xFFFFF4B8),
            fillingColor = Color(0xFFFFE57F),
            description = "Bright Meyer lemon sponge with a tangy lemon curd center."
        ),
        CakeBase(
            id = "carrot_cake",
            name = "Cozy Spiced Carrot",
            baseColor = Color(0xFFD4A373),
            fillingColor = Color(0xFFFAF6F0),
            description = "Spelt carrot sponge infused with warm cinnamon, nutmeg, and sweet cream filling."
        ),
        // Limited Editions
        CakeBase(
            id = "spring_matcha",
            name = "Uji Matcha Green Tea",
            baseColor = Color(0xFFADC178),
            fillingColor = Color(0xFFE0EEC6),
            description = "Earthy, finely-milled matcha sponge with white chocolate sweet whipped cream.",
            isLimited = true,
            limitedCollectionName = "Spring Meadow"
        ),
        CakeBase(
            id = "winter_berry",
            name = "Spiced Elderberry",
            baseColor = Color(0xFF4A154B),
            fillingColor = Color(0xFFFFC6FF),
            description = "Mulled dark elderberry sponge layered with spiced sugar syrup.",
            isLimited = true,
            limitedCollectionName = "Winter Hearth"
        )
    )

    val frostingFlavors = listOf(
        FrostingFlavor(
            id = "vanilla",
            name = "Sweet Vanilla Bean",
            color = Color(0xFFFAF6F0)
        ),
        FrostingFlavor(
            id = "strawberry",
            name = "Fresh Strawberry",
            color = Color(0xFFFFB7B2)
        ),
        FrostingFlavor(
            id = "chocolate",
            name = "Velvety Cocoa",
            color = Color(0xFF5D4037)
        ),
        FrostingFlavor(
            id = "orange",
            name = "Tangy Orange Blossom",
            color = Color(0xFFFFD1A9)
        ),
        // Limited Editions
        FrostingFlavor(
            id = "lavender_shimmer",
            name = "Lavender Dream (Shimmer)",
            color = Color(0xFFE8AEFF),
            sparkle = true,
            isLimited = true,
            limitedCollectionName = "Spring Meadow"
        ),
        FrostingFlavor(
            id = "mint_cream",
            name = "Frosted Peppermint",
            color = Color(0xFFBFFCC6),
            isLimited = true,
            limitedCollectionName = "Winter Hearth"
        )
    )

    val spatulas = listOf(
        Spatula(
            id = 1,
            name = "No. 1 Detail Pointer",
            bladeDesc = "Tiny tapered pointed tip",
            strokeWidth = 14f,
            alpha = 0.95f,
            description = "Perfect for fine flower details, tiny stems, lettering, or dot centers."
        ),
        Spatula(
            id = 2,
            name = "No. 2 Narrow Shaper",
            bladeDesc = "Slender angled edge",
            strokeWidth = 28f,
            alpha = 0.85f,
            description = "Great for clean borders, star edges, or precise parallel lines."
        ),
        Spatula(
            id = 3,
            name = "No. 3 Swoop Sculptor",
            bladeDesc = "Teardrop curved paddle",
            strokeWidth = 52f,
            alpha = 0.75f,
            description = "Creates beautiful organic buttercream swoops, ripples, and waves of frosting."
        ),
        Spatula(
            id = 4,
            name = "No. 4 Velvet Smoother",
            bladeDesc = "Wide straight blade",
            strokeWidth = 85f,
            alpha = 0.65f,
            description = "Sweeps broad areas for smooth base-coats, background blending, or soft coverage."
        ),
        Spatula(
            id = 5,
            name = "No. 5 Ridge Master",
            bladeDesc = "Asymmetric ridged scraper",
            strokeWidth = 120f,
            alpha = 0.60f,
            description = "Generates bold textured sweeps with artistic, thick visual ridges."
        )
    )

    val toppings = listOf(
        Topping(id = "rainbow_sprinkles", name = "Pastel Sprinkles"),
        Topping(id = "blossom_stamp", name = "Classic Buttercream Daisy (Stamp)"),
        Topping(id = "red_cherry", name = "Glazed Cherry"),
        Topping(id = "mint_leaf", name = "Fresh Mint Sprig"),
        Topping(id = "strawberry_slice", name = "Fresh Strawberry Half"),
        Topping(id = "blueberry", name = "Plump Blueberries"),
        // Limited Editions
        Topping(id = "shimmer_stars", name = "Golden Star Dust", isLimited = true, limitedCollectionName = "Spring Meadow"),
        Topping(id = "holly_berry", name = "Sugar Holly Leaves", isLimited = true, limitedCollectionName = "Winter Hearth")
    )
}
