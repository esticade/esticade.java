package io.esticade;

public class TestBean {
    public int integer;
    public double floatingPoint;
    public String string;

    public TestBean(){}

    public TestBean(int integer, double floatingPoint, String string) {
        this.integer = integer;
        this.floatingPoint = floatingPoint;
        this.string = string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestBean testBean = (TestBean) o;

        if (integer != testBean.integer) return false;
        if (Double.compare(testBean.floatingPoint, floatingPoint) != 0) return false;
        return string != null ? string.equals(testBean.string) : testBean.string == null;
    }
}
