package io.github.purpleposeidon;

import net.minecraft.world.chunk.Chunk;

public interface IPatch {
    IPatch NULL = new IPatch() {
        @Override
        public void apply(Chunk chunk, int cx, int y, int cz) {
            // Arrgh!
        }
    };

    void apply(Chunk chunk, int cx, int y, int cz);
}
