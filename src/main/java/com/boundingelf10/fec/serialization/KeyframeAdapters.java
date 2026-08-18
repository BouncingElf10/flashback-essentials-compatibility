package com.boundingelf10.fec.serialization;

import com.boundingelf10.fec.cosmetics.CosmeticKeyframe;
import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.function.Function;

public final class KeyframeAdapters implements TypeAdapterFactory {

	private static final String EMOTE = "com.boundingelf10.fec.emotes.EmoteKeyframe";
	private static final String COSMETIC = "com.boundingelf10.fec.cosmetics.CosmeticKeyframe";

	@Override
	@SuppressWarnings("unchecked")
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		String name = type.getRawType().getName();

		if (EMOTE.equals(name)) {
			return (TypeAdapter<T>) new Tree<>(gson, EmoteKeyframe::toJson, EmoteKeyframe::fromJson).nullSafe();
		}

		if (COSMETIC.equals(name)) {
			return (TypeAdapter<T>) new Tree<>(gson, CosmeticKeyframe::toJson, CosmeticKeyframe::fromJson).nullSafe();
		}

		return null;
	}

	private static final class Tree<T> extends TypeAdapter<T> {
		private final Gson gson;
		private final Function<T, JsonObject> write;
		private final Function<JsonObject, T> read;
		private TypeAdapter<JsonElement> elements;

		Tree(Gson gson, Function<T, JsonObject> write, Function<JsonObject, T> read) {
			this.gson = gson;
			this.write = write;
			this.read = read;
		}

		@Override
		public void write(JsonWriter out, T value) throws IOException {
			JsonElement element = this.write.apply(value);
			this.elements().write(out, element == null ? JsonNull.INSTANCE : element);
		}

		@Override
		public T read(JsonReader in) throws IOException {
			JsonElement element = this.elements().read(in);
			return element == null || !element.isJsonObject() ? null : this.read.apply(element.getAsJsonObject());
		}

		private TypeAdapter<JsonElement> elements() {
			if (this.elements == null) {
				this.elements = this.gson.getAdapter(JsonElement.class);
			}

			return this.elements;
		}
	}
}
