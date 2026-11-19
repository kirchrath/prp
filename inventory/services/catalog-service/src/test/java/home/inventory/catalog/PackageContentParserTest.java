package home.inventory.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.catalog.article.ContentUnit;
import home.inventory.catalog.article.PackageContent;
import home.inventory.catalog.article.PackageContentParser;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PackageContentParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "500 g;500;GRAM", "500g;500;GRAM", "1 kg;1000;GRAM", "1,5 kg e;1500;GRAM", "250 gr;250;GRAM",
            "500 mg;0.5;GRAM", "1 L;1000;MILLILITER", "0.75 l;750;MILLILITER", "33 cl;330;MILLILITER",
            "5 dl;500;MILLILITER", "750 ml;750;MILLILITER", "6 x 1,5 l;9000;MILLILITER", "4×125g;500;GRAM",
            "10 Stück;10;PIECE", "6 Stk.;6;PIECE", "12 st;12;PIECE", "4 pcs;4;PIECE", "1 pc;1;PIECE",
            "Packung mit 400 g Inhalt;400;GRAM"})
    void parses(String text, String amount, ContentUnit unit) {
        assertThat(PackageContentParser.parse(text)).contains(new PackageContent(new BigDecimal(amount).stripTrailingZeros(), unit));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "eine Packung", "500", "10 gläser", "0 g", "3 stangen"})
    void ignoresUnknownTexts(String text) {
        assertThat(PackageContentParser.parse(text)).isEmpty();
    }
}
