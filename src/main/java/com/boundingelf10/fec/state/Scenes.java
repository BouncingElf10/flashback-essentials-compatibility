package com.boundingelf10.fec.state;

import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;

import java.util.function.Function;

public final class Scenes {

	private Scenes() { }

	public static <T> T read(EditorState editorState, long stamp, Function<EditorScene, T> reader) {
		try {
			return reader.apply(editorState.getCurrentScene(stamp));
		} catch (IllegalStateException notOurStamp) {
			long own = editorState.acquireRead();

			try {
				return reader.apply(editorState.getCurrentScene(own));
			} finally {
				editorState.release(own);
			}
		}
	}
}
