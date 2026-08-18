package com.boundingelf10.fec.state;

import com.boundingelf10.fec.ext.EssentialStateHolder;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public final class EssentialState {

	private static final EssentialReplayState EMPTY = new EssentialReplayState();

	private EssentialState() { }

	public static EssentialReplayState of(EditorState editorState) {
		return ((EssentialStateHolder) editorState).fec$essential();
	}

	@Nullable
	public static EssentialReplayState current() {
		EditorState editorState = EditorStateManager.getCurrent();
		return editorState == null ? null : of(editorState);
	}

	public static EssentialReplayState currentOrEmpty() {
		EssentialReplayState state = current();
		return state == null ? EMPTY : state;
	}

	public static void edit(Consumer<EssentialReplayState> change) {
		EditorState editorState = EditorStateManager.getCurrent();

		if (editorState == null) {
			return;
		}

		change.accept(((EssentialStateHolder) editorState).fec$essential());
		editorState.markDirty();
	}
}
