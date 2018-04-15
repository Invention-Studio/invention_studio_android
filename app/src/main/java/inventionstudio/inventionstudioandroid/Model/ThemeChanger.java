package inventionstudio.inventionstudioandroid.Model;

import android.widget.ListView;

import inventionstudio.inventionstudioandroid.R;

/**
 * Created by Maxwell on 2/27/2018.
 * NOT CURRENTLY IMPLEMENTED
 */

public class ThemeChanger {
    public static int currentTheme;
    public final static int THEME_LIGHT = 0;
    public final static int THEME_DARK = 1;

    // Method used to switch the theme
    public static void apply(int theme) {
        switch(theme) {
            case THEME_LIGHT:
                currentTheme = R.style.IS_Light;
                break;
        }
    }
}
