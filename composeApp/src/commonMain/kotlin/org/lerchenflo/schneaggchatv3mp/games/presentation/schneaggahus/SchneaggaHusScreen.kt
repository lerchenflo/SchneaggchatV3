package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.House
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.viewmodel.koinViewModel
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

@Composable
fun SchneaggaHusScreenRoot() {

    val viewmodel = koinViewModel<SchneaggaHusViewmodel>()

    val state by viewmodel.state.collectAsStateWithLifecycle()

    SchneaggaHusScreen(
        state = state,
        onAction = viewmodel::onAction
    )


}


val SCHNEAGGHUS_GRID_SIZE = 20
val SCHNEAGGHUS_SPEED = 0.02f

@Composable
private fun SchneaggaHusScreen(
    state: SchneaggaHusState,
    onAction: (SchneaggaHusAction) -> Unit
) {

    BoxWithConstraints {
        val tileSize = maxWidth / SCHNEAGGHUS_GRID_SIZE

        //Spawnpunkt
        Box(modifier = Modifier
            .offset(tileSize, tileSize)
            .size(tileSize)
        ) {
            Icon(
                imageVector = Icons.Default.House,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.fillMaxSize()
            )
        }

        for (i in 1 .. SCHNEAGGHUS_GRID_SIZE) {
            for (j in 1 .. SCHNEAGGHUS_GRID_SIZE) {
                Box(
                    modifier = Modifier
                        .offset(i * tileSize, j * tileSize)
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = Color.Gray,
                        )
                )
            }
        }

        state.schneaggaPathList.forEach { path ->
            Box(modifier = Modifier
                .offset(x = path.position.x * tileSize, y = path.position.y * tileSize)
                .size(tileSize)
            ) {


                if (path.isStraight()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .rotate(if (path.startPoint == DIRECTION.WEST || path.startPoint == DIRECTION.EAST) 90f else 0f)
                            .fillMaxHeight()
                            .fillMaxWidth(0.35f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)

                    )
                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(path.rotation())

                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxHeight(0.675f)
                                .fillMaxWidth(0.35f)
                                .background(MaterialTheme.colorScheme.primary)

                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(0.35f)
                                .fillMaxWidth(0.675f)
                                .background(MaterialTheme.colorScheme.primary)

                        )
                    }

                }

                if (path.toggleOptions != null) {
                    Icon(
                        Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(0.5f)
                            .clickable {
                                onAction(SchneaggaHusAction.OnConsecrationClick(path.position))
                            }
                            .background(Color.Transparent),
                        tint = Color.Magenta
                    )
                }
            }
        }

        state.schneagghusList.forEach { hus ->
            Box(modifier = Modifier
                .offset(x = hus.position.x * tileSize, y = hus.position.y * tileSize)
                .size(tileSize)
            ) {
                Icon(
                    Icons.Default.House,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = hus.color
                )
            }
        }

        state.schneaggList.forEach { schneagg ->
            Box(modifier = Modifier
                .offset(x = schneagg.position.x * tileSize, y = schneagg.position.y * tileSize)
                .size(tileSize)
            ) {
                Icon(
                    imageResource(Res.drawable.icon_schneagg_alternative),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = schneagg.color
                )
            }
        }


    }


//    Canvas(modifier = Modifier.fillMaxSize()) {
//        // Calculate the size of a single grid cell in pixels
//        val cellWidth = size.width / GRID_WIDTH
//        val cellHeight = size.height / GRID_HEIGHT
//
//        state.schneaggList.forEach { schneagg ->
//            // Calculate the pixel offset based on grid position
//            val xOffset = schneagg.position.x * cellWidth
//            val yOffset = schneagg.position.y * cellHeight
//
//            // Paint the rectangle
//            drawRect(
//                color = schneagg.color, // e.g., Color.Red
//                topLeft = Offset(x = xOffset, y = yOffset),
//                size = Size(width = cellWidth, height = cellHeight)
//            )
//
//        }
//    }
}