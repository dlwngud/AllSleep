package com.wngud.allsleep.ui.auth.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.ui.theme.FontSize
import com.wngud.allsleep.ui.theme.OnSurfaceVariant

@Composable
fun AuthFooter(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val termsUrl = "https://www.notion.so/AllSleep-33892d66363680faadc6e53cd5016e35"
    val privacyUrl = "https://www.notion.so/AllSleep-33892d66363680bb8c2de90e9a7cc4e2"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "이용약관",
            fontSize = FontSize.bodySmall,
            color = OnSurfaceVariant,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { uriHandler.openUri(termsUrl) }
                .padding(8.dp)
        )
        
        Text(
            text = "·",
            fontSize = FontSize.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Text(
            text = "개인정보 처리방침",
            fontSize = FontSize.bodySmall,
            color = OnSurfaceVariant,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { uriHandler.openUri(privacyUrl) }
                .padding(8.dp)
        )
    }
}
