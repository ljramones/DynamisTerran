module org.dynamisterrain.test {
    requires org.dynamisterrain.api;
    requires org.junit.jupiter.api;

    exports org.dynamisterrain.test;
    exports org.dynamisterrain.test.mock;
    exports org.dynamisterrain.test.harness;
    exports org.dynamisterrain.test.assertions;
    exports org.dynamisterrain.test.synthetic;
}
