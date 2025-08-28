package com.Bruttt.brutalauth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Login {

    private final Context context;
    private final Utils utils;

    private EditText etUsername;
    private EditText etPassword;
    private EditText etLicense;
    private Button actionButton;

    private View loadingOverlay;
    private ProgressBar loadingSpinner;

    private boolean isRegisterMode = false;

    private static final String APPLICATION_ID = "46a9c1fa-bd43-47f6-9745-d1325296e57a";
    private static final String BASE_URL       = "https://api.brutalauth.site";

    private static final String PREF_NAME   = "LoginPrefs";
    private static final String KEY_LICENSE = "license_key";
    private static final String KEY_USER    = "username";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());

    private final BrutalAuth auth;

    public Login(Context glob_Context) {
        context = glob_Context;
        utils = new Utils(context);
        auth = new BrutalAuth(context.getApplicationContext(), APPLICATION_ID, BASE_URL);
        buildUI();
    }

    private void buildUI() {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedUser = pref.getString(KEY_USER, "");
        String savedKey  = pref.getString(KEY_LICENSE, "");

        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackground(makeBackdrop());

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(utils.FixDP(12), utils.FixDP(12), utils.FixDP(12), utils.FixDP(12));

        // Branding
        TextView brand = new TextView(context);
        brand.setText("🔐 BrutalAuth");
        brand.setTextColor(Color.WHITE);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setTextSize(22);
        brand.setGravity(Gravity.CENTER);

        TextView tagline = new TextView(context);
        tagline.setText("Secure access • Clean design");
        tagline.setTextColor(0xFFB0B0B0);
        tagline.setTextSize(12);
        tagline.setGravity(Gravity.CENTER);

        LinearLayout brandBox = vstack(brand, tagline);

        // Card container
        LinearLayout card = makeCard();
        int maxWidth = utils.FixDP(420);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int cardWidth = Math.min(screenWidth - utils.FixDP(24), maxWidth);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        cardParams.topMargin = utils.FixDP(12);
        card.setLayoutParams(cardParams);

        // Header
        LinearLayout header = makeHeader();
        TextView headline = new TextView(context);
        headline.setText("Welcome");
        headline.setTextColor(Color.WHITE);
        headline.setTypeface(Typeface.DEFAULT_BOLD);
        headline.setTextSize(16);
        header.addView(headline);

        // Segmented Tabs (Login/Register)
        LinearLayout toggleBar = new LinearLayout(context);
        toggleBar.setOrientation(LinearLayout.HORIZONTAL);
        toggleBar.setGravity(Gravity.CENTER);

        TextView tabLogin = makeTab("Login", true);
        TextView tabRegister = makeTab("Register", false);

        toggleBar.addView(tabLogin, new LinearLayout.LayoutParams(0, utils.FixDP(40), 1f));
        toggleBar.addView(tabRegister, new LinearLayout.LayoutParams(0, utils.FixDP(40), 1f));

        // Inputs
        etUsername = makeInput("👤 Username", false);
        if (!savedUser.isEmpty()) etUsername.setText(savedUser);

        etPassword = makeInput("🔒 Password", true);

        etLicense = makeInput("🔑 License key (Register only)", false);
        if (!savedKey.isEmpty()) etLicense.setText(savedKey);
        etLicense.setVisibility(View.GONE);

        // CTA
        actionButton = makeCta("Login");
        runBreathing(actionButton);
        actionButton.setOnClickListener(v -> onActionClick());

        // Tab click handlers
        tabLogin.setOnClickListener(v -> switchMode(tabLogin, tabRegister, false));
        tabRegister.setOnClickListener(v -> switchMode(tabRegister, tabLogin, true));

        // Footer
        TextView foot = new TextView(context);
        foot.setText("© BrutalAuth OB50");
        foot.setTextColor(0xFF777777);
        foot.setTextSize(9);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, utils.FixDP(12), 0, utils.FixDP(12));

        // Build card content
        LinearLayout inner = vstack(
                toggleBar,
                etUsername,
                etPassword,
                etLicense,
                actionButton
        );
        inner.setPadding(utils.FixDP(12), utils.FixDP(10), utils.FixDP(12), utils.FixDP(10));

        card.addView(header);
        card.addView(inner);

        // Loading overlay
        loadingOverlay = new View(context);
        loadingOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingOverlay.setBackgroundColor(0x88000000);
        loadingOverlay.setVisibility(View.GONE);

        loadingSpinner = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                utils.FixDP(40), utils.FixDP(40), Gravity.CENTER);
        loadingSpinner.setLayoutParams(sp);
        loadingSpinner.setVisibility(View.GONE);

        // Assemble
        ((Activity) context).setContentView(root);
        column.addView(brandBox);
        column.addView(card);
        column.addView(foot);
        scrollView.addView(column);
        root.addView(scrollView);
        root.addView(loadingOverlay);
        root.addView(loadingSpinner);
    }

    private GradientDrawable makeToggleBackground(boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(utils.FixDP(8));
        gd.setStroke(utils.FixDP(1), 0x55FFFFFF);
        gd.setColor(active ? 0xFFFF4444 : 0xFF2A2A2A);
        return gd;
    }

    private void switchMode(TextView active, TextView inactive, boolean register) {
        isRegisterMode = register;
        active.setBackground(makeToggleBackground(true));
        active.setTextColor(Color.WHITE);
        inactive.setBackground(makeToggleBackground(false));
        inactive.setTextColor(0xFFAAAAAA);
        etLicense.setVisibility(register ? View.VISIBLE : View.GONE);
        animateTextChange(actionButton, register ? "Register" : "Login");
    }

    private void onActionClick() {
        final boolean isRegister = isRegisterMode;

        String username = safe(etUsername.getText().toString().trim());
        String password = safe(etPassword.getText().toString().trim());
        String license  = safe(etLicense.getText().toString().trim());

        if (username.isEmpty()) { fieldError(etUsername, "Enter username"); return; }
        if (password.isEmpty()) { fieldError(etPassword, "Enter password"); return; }
        if (isRegister && license.isEmpty()) { fieldError(etLicense, "Enter license key"); return; }

        setUiEnabled(false);

        io.execute(() -> {
            BrutalAuth.AuthResult res = isRegister
                    ? auth.registerUser(license, username, password)
                    : auth.loginUser(username, password);

            main.post(() -> {
                setUiEnabled(true);
                if (res.success) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(KEY_USER, username);
                    if (isRegister) editor.putString(KEY_LICENSE, license);
                    editor.apply();

                    toast(isRegister ? "Registration successful!" : "Login successful!");

                    // Go to Auth page
                    Intent intent = new Intent(context, AuthActivity.class);
                    intent.putExtra("username", username);
                    context.startActivity(intent);
                    ((Activity) context).finish();

                } else {
                    toast((isRegister ? "Register failed: " : "Login failed: ") + res.message);
                }
            });
        });
    }

    private void setUiEnabled(boolean enabled) {
        etUsername.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etLicense.setEnabled(enabled);
        actionButton.setEnabled(enabled);
        loadingOverlay.setVisibility(enabled ? View.GONE : View.VISIBLE);
        loadingSpinner.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    // --------- tiny UI helpers below ----------
    private TextView makeTab(String text, boolean active) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(14);
        tv.setBackground(makeToggleBackground(active));
        tv.setTextColor(active ? Color.WHITE : 0xFFAAAAAA);
        return tv;
    }

    private EditText makeInput(String hint, boolean password) {
        EditText et = new EditText(context);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, utils.FixDP(46));
        p.topMargin = utils.FixDP(8);
        et.setLayoutParams(p);
        et.setHint(hint);
        et.setHintTextColor(0xFFBBBBBB);
        et.setTextColor(Color.WHITE);
        et.setPadding(utils.FixDP(12), 0, utils.FixDP(12), 0);
        et.setTextSize(14);
        et.setBackground(makeInputBackground());
        if (password) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            et.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        return et;
    }

    private Button makeCta(String text) {
        GradientDrawable base = new GradientDrawable();
        base.setColor(0xFFFF4444);
        base.setCornerRadius(utils.FixDP(12));

        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(0x33FFFFFF), base, null
        );

        Button b = new Button(context);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(15);
        b.setBackground(ripple);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, utils.FixDP(48));
        b.setLayoutParams(lp);
        b.setAllCaps(false);

        b.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });

        return b;
    }

    private void runBreathing(View v) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.04f, 1f);
        sx.setRepeatCount(ObjectAnimator.INFINITE);
        sx.setDuration(1400);
        sx.start();
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.04f, 1f);
        sy.setRepeatCount(ObjectAnimator.INFINITE);
        sy.setDuration(1400);
        sy.start();
    }

    private void animateTextChange(final TextView tv, final String to) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f);
        fadeOut.setDuration(120);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                tv.setText(to);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tv, "alpha", 0f, 1f);
                fadeIn.setDuration(120);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void fieldError(EditText et, String msg) {
        toast(msg);
        et.requestFocus();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable makeBackdrop() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0E0E12, 0xFF1A1B21});
    }

    private LinearLayout makeHeader() {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(utils.FixDP(12), utils.FixDP(12), utils.FixDP(12), utils.FixDP(8));
        header.setBackground(makeLinearGrad(0xFFFF5A5A, 0xFFB30000));
        return header;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeGlassNeon());
        return card;
    }

    private GradientDrawable makeLinearGrad(int start, int end) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, end});
        gd.setCornerRadius(utils.FixDP(18));
        return gd;
    }

    private LayerDrawable makeGlassNeon() {
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x33222222, 0x55222222});
        glass.setCornerRadius(utils.FixDP(20));
        glass.setStroke(utils.FixDP(1), 0x33FFFFFF);

        GradientDrawable glow = new GradientDrawable();
        glow.setColor(Color.TRANSPARENT);
        glow.setCornerRadius(utils.FixDP(24));
        glow.setStroke(utils.FixDP(2), 0x55FF4444);

        InsetDrawable insetGlow = new InsetDrawable(glow, utils.FixDP(-4));
        return new LayerDrawable(new android.graphics.drawable.Drawable[]{glass, insetGlow});
    }

    private GradientDrawable makeInputBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF2B2B2B);
        gd.setCornerRadius(utils.FixDP(10));
        gd.setStroke(utils.FixDP(1), 0x44FFFFFF);
        return gd;
    }

    private LinearLayout vstack(View... children) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (View c : children) if (c != null) ll.addView(c);
        return ll;
    }
}
