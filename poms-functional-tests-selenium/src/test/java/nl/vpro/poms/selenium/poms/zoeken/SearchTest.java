package nl.vpro.poms.selenium.poms.zoeken;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import nl.vpro.poms.selenium.pages.Search;
import nl.vpro.poms.selenium.poms.AbstractTest;
import nl.vpro.poms.selenium.util.DateFactory;
import nl.vpro.poms.selenium.util.WebDriverFactory;
import nl.vpro.poms.selenium.util.types.AvType;
import nl.vpro.poms.selenium.util.types.MediaType;

import static org.junit.Assert.assertTrue;

public class SearchTest extends AbstractTest {

    public SearchTest(@Nonnull WebDriverFactory.Browser browser) {
        super(browser);
    }
    Search search;

    @Before
    public void setup() {
        login().speciaalVf();
        search = new Search(webDriverUtil);
    }

    @After
    public void logout() {
        super.logout();
    }

    @Test
    public void testSearch() {
        search.enterQuery("klusjesmannen");
        assertFound("De Klusjesmannen");
    }

    @Test
    public void testSearchTwice() {
        search.enterQuery("klusjesmannen");
        search.closeTab();
        search.enterQuery("klusjesmannen");
        assertFound( "De Klusjesmannen");
    }

    @Test
    public void testSearchWithAvType() {
        search.enterQuery("klusjesmannen");
        search.selectOptionFromMenu("avType", AvType.AUDIO.getType());
        search.clickZoeken();
        assertFound( "A je to! 40 jaar Buurman en Buurman");
    }

    @Test
    public void testSearchWithClip() {
        search.enterQuery("klusjesmannen");
        search.selectOptionFromMenu("avType", AvType.VIDEO.getType());
        search.selectOptionFromMenu("MediaType", MediaType.CLIP.getType());
        search.clickZoeken();
        assertFound("Casa Jepie Makandra");
    }

    @Test
    public void testSearchWithOmroep() {
        search.enterQuery("zembl");
        search.selectOptionFromMenu("Omroepen", "VARA");
        search.clickZoeken();
        assertFound("Zembla");

    }

    @Test
    public void testSearchWithCriteria() {
        search.enterQuery("zembla");
        search.selectOptionFromMenu("Criteria", "Niet op radio/tv");
        search.clickZoeken();
        assertFound("Zembla");
    }

    @Test
    public void testWithSorteerData() {
        search.enterQuery("zembla");
        search.enterSorteerdatumDates("01-01-2001", DateFactory.getToday());
        assertFound("Zembla");
    }

    @Test
    public void testWithUitzendData() {
        search.enterQuery("zembla");
        search.enterUitzenddatumDates("01-01-2001", DateFactory.getToday());
        assertFound("Zembla");
    }

    @Test
    public void testWithGewijzigdData() {
        Search search = new Search(webDriverUtil);
        search.enterQuery("zembla");
        search.enterGewijzigdDates("01-01-2001", DateFactory.getToday());
        assertFound("Zembla");
    }

    @Test
    public void testSearchWithAangemaaktData() {
        search.enterQuery("zembla");
        search.enterUitzenddatumDates("01-01-2001", DateFactory.getToday());
        assertFound("Zembla");
    }

    @Test
    public void testSearchWithTags() {
        search.enterQuery("Dokter Pop");
        search.enterTags("secret garden");
        search.clickZoeken();
        assertFound("Dokter Pop: een liedje van Springsteen dat lijkt o");
    }

    @Test
    public void testSearchWithTitleAndOmroep() {
        search.selectOptionFromMenu("Omroepen", "AVRO");
        search.enterQuery("pluk");
        assertFound("Pluk van de Petteflet");
    }

    @Test
    public void testWithTitleSuggestion() {
        search.enterQuery("pluk");
        List<WebElement> suggestions = search.getSuggestions("pluk");
        Assert.assertFalse(suggestions.isEmpty());
    }

    @Test
    public void testAddAndRemoveColumn() {
        search.addOrRemoveColumn("MID");
        assertTrue(search.isColumnSelectorChecked("MID"));
        driver.navigate().refresh();
        assertTrue(search.isColumnSelectorChecked("MID"));
        search.addOrRemoveColumn("MID");
        Assert.assertFalse(search.isColumnSelectorChecked("MID"));
    }


    protected void assertFound(String searchText) {
        assertTrue("Items '" + searchText + "'not found", search.itemFound(searchText));

    }

}
