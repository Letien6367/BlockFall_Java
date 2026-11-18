package tetris.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TetrominoBag {
    private static final List<TetrominoType> ALL_TYPES = List.of(TetrominoType.values());

    private final List<TetrominoType> bag = new ArrayList<>(ALL_TYPES);
    private int nextIndex = bag.size();

    public TetrominoType next() {
        if (nextIndex == bag.size()) {
            refill();
        }
        return bag.get(nextIndex++);
    }

    public void reset() {
        refill();
    }

    private void refill() {
        bag.clear();
        bag.addAll(ALL_TYPES);
        Collections.shuffle(bag);
        nextIndex = 0;
    }
}
