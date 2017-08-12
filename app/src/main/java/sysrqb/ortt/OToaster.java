package sysrqb.ortt;

import android.content.Context;
import android.widget.Toast;

public class OToaster {
    static public void createToast(Context c, CharSequence message) {
        Toast toast = Toast.makeText(c, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
