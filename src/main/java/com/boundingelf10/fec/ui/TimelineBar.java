package com.boundingelf10.fec.ui;

import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;

import imgui.moulberry90.ImDrawList;

import java.util.Map;
import java.util.TreeMap;

public final class TimelineBar {
	private static final float OPEN_ENDED = Integer.MAX_VALUE / 2.0F;

	private float spanTicks = -1.0F;

	public float widthInTicks(int durationTicks) {
		if (this.spanTicks > 0.0F) {
			return this.spanTicks;
		}

		return durationTicks > 0 ? durationTicks : -1.0F;
	}

	public void draw(ImDrawList drawList, int keyframeSize, float x, float y, int colour, float timelineScale,
		float minTimelineX, float maxTimelineX, int tick, TreeMap<Integer, Keyframe> keyframeTimes,
		int durationTicks, int bodyColour, String label) {

		Map.Entry<Integer, Keyframe> next = keyframeTimes.ceilingEntry(tick + 1);
		float span = next == null ? OPEN_ENDED : next.getKey() - tick;

		if (durationTicks > 0) {
			span = Math.min(span, durationTicks);
		}

		float endX = Math.min(maxTimelineX, x + span / timelineScale);
		float shortest = keyframeSize * timelineScale;

		if (span < shortest) {
			span = shortest;
			endX = Math.max(endX, x + keyframeSize);
		}

		this.spanTicks = span;

		int alpha = colour & 0xFF000000;
		drawList.addRectFilled(x, y - keyframeSize, endX, y + keyframeSize, alpha | bodyColour);
		drawList.addRect(x, y - keyframeSize - 1.0F, endX, y + keyframeSize + 1.0F, colour);

		float labelX = Math.max(x, minTimelineX) + 3.0F;

		if (endX - labelX > ImGuiHelper.calcTextWidth(label) + 3.0F) {
			drawList.addText(labelX, y - keyframeSize, colour, label);
		}
	}
}
