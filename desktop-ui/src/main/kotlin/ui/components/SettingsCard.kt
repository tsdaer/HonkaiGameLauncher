package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = Modifier
                .height(80.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth().shadow(4.dp,
                shape = RoundedCornerShape(5.dp)),
        shape = RoundedCornerShape(5.dp),

    ) {
        Row(modifier = Modifier.padding(start = 8.dp),verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSurface
            )
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ){
                Box(modifier = Modifier.padding(end = 8.dp))
                {
                    content()
                }
            }

        }

    }

}