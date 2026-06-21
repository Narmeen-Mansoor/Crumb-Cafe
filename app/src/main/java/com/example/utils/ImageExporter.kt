package com.example.utils

import android.content.Context
import android.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.example.data.CakeBase
import com.example.data.FrostingFlavor
import com.example.viewmodel.PaintStroke
import com.example.viewmodel.PlacedTopping
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageExporter {

    fun generatePolaroidCard(
        context: Context,
        base: CakeBase,
        flavor: FrostingFlavor,
        strokes: List<PaintStroke>,
        placedToppings: List<PlacedTopping>,
        customNote: String,
        guestName: String
    ): Bitmap {
        // High-fidelity card dimensions (600x800 polaroid ratio)
        val cardWidth = 600
        val cardHeight = 800
        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw warm Cozy Sand background for the photo tabletop
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FAF6F0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

        // 2. Draw white Polaroid Card Base with soft shadow
        val cardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val shadowPaint = Paint().apply {
            color = Color.parseColor("#15000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw shadow first
        canvas.drawRoundRect(25f, 35f, cardWidth.toFloat() - 25f, cardHeight.toFloat() - 25f, 16f, 16f, shadowPaint)
        // Draw card body
        canvas.drawRoundRect(20f, 30f, cardWidth.toFloat() - 20f, cardHeight.toFloat() - 30f, 16f, 16f, cardPaint)

        // 3. Inner image photo boundary (top-aligned, matching typical polaroid proportions)
        val imgLeft = 45f
        val imgTop = 55f
        val imgRight = cardWidth.toFloat() - 45f
        val imgBottom = 540f
        val imgWidth = imgRight - imgLeft
        val imgHeight = imgBottom - imgTop

        // Photo inner shadow/border
        val photoBgPaint = Paint().apply {
            color = Color.parseColor("#F4EFE9") // Tonal Sand mockup table inside the photo
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(imgLeft, imgTop, imgRight, imgBottom, photoBgPaint)

        // Draw presentation plate (Ice Blue) centered inside the image
        val centerX = imgLeft + imgWidth / 2f
        val centerY = imgTop + imgHeight / 2f
        val platePaint = Paint().apply {
            color = Color.parseColor("#D8E2DC") // Ice Blue plate
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawOval(centerX - 160f, centerY - 140f, centerX + 160f, centerY + 140f, platePaint)
        
        // Draw plate inner rim for depth
        val plateRimPaint = Paint().apply {
            color = Color.parseColor("#C3CECB")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawOval(centerX - 120f, centerY - 100f, centerX + 120f, centerY + 100f, plateRimPaint)

        // Coordinate scaling: from [0, 1] normalized bounds to inner image bounds
        val mapX = { normX: Float -> imgLeft + normX * imgWidth }
        val mapY = { normY: Float -> imgTop + normY * imgHeight }

        // --- CAKE RECONSTRUCTION DRAWING ---
        // Top-down horizontal view pointing left
        // Triangle vertex positions (Normalized)
        val ptLeft = Pair(0.25f, 0.5f)
        val ptTopRight = Pair(0.75f, 0.32f)
        val ptBottomRight = Pair(0.75f, 0.68f)

        // A. Draw Cake Side (Sponge slice wall) going down by 50px
        val sideHeight = 50f
        val sideBasePaint = Paint().apply {
            color = base.baseColor.toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val fillingPaint = Paint().apply {
            color = base.fillingColor.toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Left triangle wall (from bottom edge of top surface downwards)
        val sidePath = Path().apply {
            moveTo(mapX(ptLeft.first), mapY(ptLeft.second))
            lineTo(mapX(ptBottomRight.first), mapY(ptBottomRight.second))
            lineTo(mapX(ptBottomRight.first), mapY(ptBottomRight.second) + sideHeight)
            lineTo(mapX(ptLeft.first), mapY(ptLeft.second) + sideHeight)
            close()
        }
        canvas.drawPath(sidePath, sideBasePaint)

        // Filling lines within the cake wall (sweet cream stripe)
        val fillingPath = Path().apply {
            moveTo(mapX(ptLeft.first), mapY(ptLeft.second) + sideHeight * 0.4f)
            lineTo(mapX(ptBottomRight.first), mapY(ptBottomRight.second) + sideHeight * 0.4f)
            lineTo(mapX(ptBottomRight.first), mapY(ptBottomRight.second) + sideHeight * 0.6f)
            lineTo(mapX(ptLeft.first), mapY(ptLeft.second) + sideHeight * 0.6f)
            close()
        }
        canvas.drawPath(fillingPath, fillingPaint)

        // Side crust/edge highlights to show crumb texture
        val texturePaint = Paint().apply {
            color = Color.parseColor("#15000000")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(mapX(ptLeft.first), mapY(ptLeft.second), mapX(ptLeft.first), mapY(ptLeft.second) + sideHeight, texturePaint)
        canvas.drawLine(mapX(ptBottomRight.first), mapY(ptBottomRight.second), mapX(ptBottomRight.first), mapY(ptBottomRight.second) + sideHeight, texturePaint)

        // B. Draw Top Cake Surface (Triangle)
        val topSurfacePath = Path().apply {
            moveTo(mapX(ptLeft.first), mapY(ptLeft.second))
            lineTo(mapX(ptTopRight.first), mapY(ptTopRight.second))
            lineTo(mapX(ptBottomRight.first), mapY(ptBottomRight.second))
            close()
        }
        canvas.drawPath(topSurfacePath, sideBasePaint)

        // C. Draw Buttercream Frosting drawing strokes clipped inside the Top Surface!
        canvas.save()
        canvas.clipPath(topSurfacePath)

        // If the user selected chocolate or strawberry flavor, we can pre-coat the surface slightly as a base layer
        val baseTintPaint = Paint().apply {
            color = flavor.color.toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 100 // translucent base glaze crumbs
        }
        canvas.drawPath(topSurfacePath, baseTintPaint)

        // Render user-decorating strokes
        strokes.forEach { stroke ->
            val strokePaint = Paint().apply {
                color = stroke.color.toArgb()
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.strokeWidth * (imgWidth / 450f) // scale with exported card boundaries
                alpha = (stroke.alpha * 255).toInt()
                isAntiAlias = true
            }

            if (stroke.isDaisy) {
                // Draw a beautiful custom Daisy Stamp at the first recorded coordinate
                if (stroke.points.isNotEmpty()) {
                    val p = stroke.points.first()
                    val stampX = mapX(p.x)
                    val stampY = mapY(p.y)
                    drawCustomDaisy(canvas, stampX, stampY, 15f)
                }
            } else {
                val path = Path()
                if (stroke.points.isNotEmpty()) {
                    path.moveTo(mapX(stroke.points[0].x), mapY(stroke.points[0].y))
                    for (i in 1 until stroke.points.size) {
                        path.lineTo(mapX(stroke.points[i].x), mapY(stroke.points[i].y))
                    }
                    canvas.drawPath(path, strokePaint)
                }
            }
        }
        canvas.restore()

        // D. Draw placed Topping stickers (can be anywhere, even slightly overlapping)
        placedToppings.forEach { topping ->
            val topX = mapX(topping.offset.x)
            val topY = mapY(topping.offset.y)

            when (topping.id) {
                "blossom_stamp" -> {
                    drawCustomDaisy(canvas, topX, topY, 16f)
                }
                "red_cherry" -> {
                    // Cherry ball
                    val cherryPaint = Paint().apply {
                        color = Color.parseColor("#D62246")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(topX, topY, 16f, cherryPaint)
                    // Stem
                    val stemPaint = Paint().apply {
                        color = Color.parseColor("#5D4037")
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        strokeCap = Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    canvas.drawArc(topX - 10f, topY - 30f, topX + 10f, topY, 0f, -120f, false, stemPaint)
                    // Shine
                    val shinePaint = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(topX - 5f, topY - 5f, 4f, shinePaint)
                }
                "mint_leaf" -> {
                    val leafPaint = Paint().apply {
                        color = Color.parseColor("#7F9C7D")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val leafPath = Path().apply {
                        moveTo(topX, topY)
                        quadTo(topX + 15f, topY - 15f, topX + 25f, topY - 5f)
                        quadTo(topX + 10f, topY + 15f, topX, topY)
                        close()
                    }
                    canvas.drawPath(leafPath, leafPaint)
                }
                "strawberry_slice" -> {
                    val berryOuter = Paint().apply {
                        color = Color.parseColor("#E63946")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val berryInner = Paint().apply {
                        color = Color.parseColor("#FFB7B2")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(topX, topY, 15f, berryOuter)
                    canvas.drawCircle(topX, topY, 9f, berryInner)
                }
                "blueberry" -> {
                    val bluePaint = Paint().apply {
                        color = Color.parseColor("#3B5B8C")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(topX, topY, 14f, bluePaint)
                    canvas.drawCircle(topX - 2f, topY - 2f, 11f, Paint().apply {
                        color = Color.parseColor("#4A73B2")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    })
                }
                "rainbow_sprinkles", "shimmer_stars" -> {
                    val starPaint = Paint().apply {
                        color = Color.parseColor("#FFD97D")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    drawMiniStar(canvas, topX, topY, 8f, starPaint)
                }
                "holly_berry" -> {
                    val redPaint = Paint().apply {
                        color = Color.RED
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(topX - 4f, topY, 5f, redPaint)
                    canvas.drawCircle(topX + 4f, topY - 2f, 5f, redPaint)
                }
            }
        }

        // 4. Polaroid Text Area at the bottom
        val fontTitle = Paint().apply {
            color = Color.parseColor("#4A3E3D") // Warm Earth text
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val fontSub = Paint().apply {
            color = Color.parseColor("#7F9C7D") // Warm Sage subtext
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val fontDate = Paint().apply {
            color = Color.GRAY
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Render Polaroid Texts beautifully centered under the photo frame
        val textCenter = cardWidth / 2f
        
        // Cake Slogan Title
        canvas.drawText(guestName, textCenter, 600f, fontTitle)
        
        // Guest story custom description note
        val description = if (customNote.length > 50) customNote.take(47) + "..." else customNote
        canvas.drawText(description, textCenter, 645f, fontSub)

        // Bakery branding label and dynamic Date
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        canvas.drawText("Bloom & Crumb Café • $formattedDate", textCenter, 720f, fontDate)

        // Return the beautiful final polaroid card image
        return bitmap
    }

    private fun drawCustomDaisy(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val petalPaint = Paint().apply {
            color = Color.parseColor("#FAF6F0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val centerPaint = Paint().apply {
            color = Color.parseColor("#FFD97D")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw 5 petals around the center
        val numPetals = 5
        val step = 360f / numPetals
        for (i in 0 until numPetals) {
            val angleRad = Math.toRadians((i * step).toDouble())
            val px = cx + Math.cos(angleRad).toFloat() * size
            val py = cy + Math.sin(angleRad).toFloat() * size
            canvas.drawCircle(px, py, size * 0.7f, petalPaint) // overlapping round petals
        }
        // Center crown
        canvas.drawCircle(cx, cy, size * 0.5f, centerPaint)
    }

    private fun drawMiniStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val path = Path()
        val numPoints = 5
        val angleStep = Math.PI / numPoints
        var x: Float
        var y: Float
        for (i in 0 until 10) {
            val currentRadius = if (i % 2 == 0) radius else radius * 0.4f
            val currentAngle = i * angleStep - Math.PI / 2
            x = cx + Math.cos(currentAngle).toFloat() * currentRadius
            y = cy + Math.sin(currentAngle).toFloat() * currentRadius
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
