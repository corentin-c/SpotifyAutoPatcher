package com.corentinc.screens.patcher.ui.choice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.corentinc.SpotifyAutoPatcher.R

@Composable
fun ApplicationChoice(
	onSpotifyClick: () -> Unit ,
	onYoutubeMusicClick: () -> Unit
) {
	Surface {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.systemBarsPadding()
				.padding(50.dp),
			verticalArrangement = Arrangement.SpaceEvenly
		) {
			Text(
				text = stringResource(R.string.application_choice_title),
				style = MaterialTheme.typography.titleLarge,
			)
			Image(
				modifier = Modifier
					.clip(shape = RoundedCornerShape(5.dp))
					.background(
						color = Color.White,
						shape = RectangleShape
					)
					.border(
						width = 1.dp,
						color = Color.Black,
						shape = RoundedCornerShape(5.dp)
					)
					.padding(10.dp)
					.fillMaxWidth()
					.clickable {
						onSpotifyClick()
					},
				painter = painterResource(R.drawable.spotify_logo),
				contentDescription = "",
				contentScale = ContentScale.FillWidth
			)
			Image(
				modifier = Modifier
					.clip(shape = RoundedCornerShape(5.dp))
					.background(
						color = Color.White,
						shape = RectangleShape
					)
					.border(
						width = 1.dp,
						color = Color.Black,
						shape = RoundedCornerShape(5.dp)
					)
					.padding(10.dp)
					.fillMaxWidth()
					.clickable {
						onYoutubeMusicClick()
					},
				painter = painterResource(R.drawable.youtube_music_logo),
				contentDescription = "",
				contentScale = ContentScale.FillWidth
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun ApplicationChoicePreview() {
	ApplicationChoice(
		onSpotifyClick = {
			// empty
		},
		onYoutubeMusicClick = {
			// empty
		}
	)
}