package gedi.util.functions;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

@FunctionalInterface
public interface BooleanUnaryOperator {

	/**
     * Applies this operator to the given operand.
     *
     * @param operand the operand
     * @return the operator result
     */
    boolean applyAsBoolean(boolean operand);

    /**
     * Returns a composed operator that first applies the {@code before}
     * operator to its input, and then applies this operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * @param before the operator to apply before this operator is applied
     * @return a composed operator that first applies the {@code before}
     * operator and then applies this operator
     * @throws NullPointerException if before is null
     *
     * @see #andThen(IntUnaryOperator)
     */
    default BooleanUnaryOperator compose(BooleanUnaryOperator before) {
        Objects.requireNonNull(before);
        return (boolean v) -> applyAsBoolean(before.applyAsBoolean(v));
    }

    /**
     * Returns a composed operator that first applies this operator to
     * its input, and then applies the {@code after} operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * @param after the operator to apply after this operator is applied
     * @return a composed operator that first applies this operator and then
     * applies the {@code after} operator
     * @throws NullPointerException if after is null
     *
     * @see #compose(IntUnaryOperator)
     */
    default BooleanUnaryOperator andThen(BooleanUnaryOperator after) {
        Objects.requireNonNull(after);
        return (boolean t) -> after.applyAsBoolean(applyAsBoolean(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * @return a unary operator that always returns its input argument
     */
    static BooleanUnaryOperator identity() {
        return t -> t;
    }
	
}
