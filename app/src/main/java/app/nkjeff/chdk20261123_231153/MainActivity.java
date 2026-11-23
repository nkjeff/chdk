package app.nkjeff.chdk20261123_231153;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREF = "daka_data";
    private final ArrayList<Item> items = new ArrayList<>();
    private final Calendar selected = Calendar.getInstance();
    private LinearLayout list;
    private TextView dateText, countText;

    private static class Item {
        String id, name;
        ArrayList<String> dates = new ArrayList<>();
        Item(String id, String name) { this.id = id; this.name = name; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        load();
        render();
    }

    private String day() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.getTime());
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setBackgroundColor(Color.BLACK);

        LinearLayout header = row();
        Button prev = button("‹");
        prev.setOnClickListener(v -> { selected.add(Calendar.DAY_OF_MONTH, -1); render(); });
        header.addView(prev, new LinearLayout.LayoutParams(dp(48), dp(48)));

        dateText = text(day(), 17);
        dateText.setGravity(Gravity.CENTER);
        header.addView(dateText, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button next = button("›");
        next.setOnClickListener(v -> { selected.add(Calendar.DAY_OF_MONTH, 1); render(); });
        header.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        LinearLayout summary = row();
        countText = text("", 13);
        summary.addView(countText, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button add = button("+");
        add.setTextSize(25);
        add.setOnClickListener(v -> addItem());
        summary.addView(add, new LinearLayout.LayoutParams(dp(48), dp(42)));
        root.addView(summary);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        refreshList();
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
        return b;
    }

    private void refreshList() {
        if (list == null) return;
        list.removeAllViews();
        int done = 0;
        for (Item item : items) if (item.dates.contains(day())) done++;
        countText.setText(done + " / " + items.size() + " 已完成");
        for (Item item : new ArrayList<>(items)) {
            LinearLayout line = row();
            CheckBox check = new CheckBox(this);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            check.setText(item.name);
            check.setTextColor(Color.WHITE);
            check.setTextSize(16);
            check.setPadding(0, dp(8), dp(8), dp(8));
            check.setChecked(item.dates.contains(day()));
            check.setOnCheckedChangeListener((v, checked) -> {
                if (checked && !item.dates.contains(day())) item.dates.add(day());
                if (!checked) item.dates.remove(day());
                save();
                refreshList();
            });
            line.addView(check, new LinearLayout.LayoutParams(0, dp(54), 1));
            TextView del = text("×", 24);
            del.setGravity(Gravity.CENTER);
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("删除「" + item.name + "」及其打卡记录？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    items.remove(item);
                    save();
                    refreshList();
                }).show());
            line.addView(del, new LinearLayout.LayoutParams(dp(42), dp(54)));
            list.addView(line);
        }
    }

    private void addItem() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xff888888);
        input.setHint("输入打卡项目");
        new AlertDialog.Builder(this).setTitle("新增打卡")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("添加", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    Item item = new Item(Long.toString(System.nanoTime()), name);
                    items.add(item);
                    save();
                    refreshList();
                }
            }).show();
    }

    private void load() {
        try {
            JSONArray array = new JSONArray(getSharedPreferences(PREF, MODE_PRIVATE).getString("items", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Item item = new Item(obj.getString("id"), obj.getString("name"));
                JSONArray dates = obj.optJSONArray("dates");
                if (dates != null) for (int j = 0; j < dates.length(); j++) item.dates.add(dates.getString(j));
                items.add(item);
            }
        } catch (Exception ignored) { items.clear(); }
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (Item item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("name", item.name);
                obj.put("dates", new JSONArray(item.dates));
                array.put(obj);
            }
            getSharedPreferences(PREF, MODE_PRIVATE).edit().putString("items", array.toString()).apply();
        } catch (Exception ignored) { }
    }
}
