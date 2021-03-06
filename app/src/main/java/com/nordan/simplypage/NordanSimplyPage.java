package com.nordan.simplypage;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.nordan.simplypage.dto.AccountElement;
import com.nordan.simplypage.dto.BaseElement;
import com.nordan.simplypage.dto.CheckBoxElement;
import com.nordan.simplypage.dto.EditableTextElement;
import com.nordan.simplypage.dto.ExtendableElement;
import com.nordan.simplypage.dto.PageElement;
import com.nordan.simplypage.dto.SeekBarElement;
import com.nordan.simplypage.dto.SingleChoiceElement;
import com.nordan.simplypage.dto.SwitchElement;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import lombok.NonNull;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class NordanSimplyPage {

    private final Activity activity;
    private final LayoutInflater layoutInflater;
    private final ConstraintLayout pageView;
    private final NordanSimplyPagePatterns nordanSimplyPagePatterns;
    private boolean isHideSeparators = false;

    public NordanSimplyPage(Activity activity) {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.pageView = (ConstraintLayout) layoutInflater.inflate(R.layout.nordan_page_activity, null);
        nordanSimplyPagePatterns = new NordanSimplyPagePatterns(activity);
        pageView.setSaveEnabled(true);
        pageView.setSaveFromParentEnabled(true);
    }

    public NordanSimplyPage hideSeparators(boolean hide) {
        isHideSeparators = hide;
        return this;
    }

    public NordanSimplyPage addEmptyItem() {
        return addEmptyItem(64);
    }

    public NordanSimplyPage addEmptyItem(int height) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, activity.getResources().getDisplayMetrics());
        RelativeLayout view = new RelativeLayout(activity);
        LayoutParams layoutParams = new LayoutParams(MATCH_PARENT, px);
        view.setLayoutParams(layoutParams);
        ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
        return this;
    }

    public NordanSimplyPage addEditableTextItem(EditableTextElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            final boolean[] isResize = {false};
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.edittext_item_view, null);
            RelativeLayout headerView = view.findViewById(R.id.relative_header);
            ImageView headerImageLeftSide = headerView.findViewById(R.id.image_left);
            ImageView headerImageRightSide = headerView.findViewById(R.id.image_right);
            MaterialTextView headerTextItem = headerView.findViewById(R.id.item_text);
            MaterialTextView headerSubTextItem = headerView.findViewById(R.id.item_subtext);
            TextInputEditText editText = view.findViewById(R.id.item_edittext);
            TextInputLayout textInputLayout = view.findViewById(R.id.edittext_layout);
            headerSubTextItem.setVisibility(View.VISIBLE);
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .map(resId -> {
                        headerImageRightSide.setVisibility(View.VISIBLE);
                        return Glide.with(activity).load(resId).into(headerImageRightSide);
                    }).orElseGet(() -> Glide.with(activity).load(R.drawable.ic_twotone_edit_24).into(headerImageRightSide));
            Optional.of(element.getLeftSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(headerImageLeftSide);
                        headerImageLeftSide.setVisibility(View.VISIBLE);
                    });
            headerTextItem.setText(element.getTitle());
            Optional.ofNullable(element.getHelperTextParams()).ifPresent(helperText -> {
                textInputLayout.setHelperTextEnabled(true);
                textInputLayout.setHelperText(helperText);
            });
            headerSubTextItem.setText(MessageFormat.format(element.getSubText(), element.getTextParams()));
            OnEditTextChangeValueListener onEditTextChangeValueListener = element.getOnEditTextChangeValueListener();
            headerView.setOnClickListener(v -> {
                String editableText = Optional.of(editText)
                        .map(AppCompatEditText::getText)
                        .map(Objects::toString)
                        .orElse("");
                if (!isResize[0]) {
                    isResize[0] = true;
                    String subText = headerSubTextItem.getText().toString();
                    for (int i = 0; i < element.getTextParams().length; i++) {
                        subText = subText.replace(element.getTextParams()[i], "{" + i + "}");
                    }
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    textInputLayout.setVisibility(View.VISIBLE);
                    editText.setText(subText);
                    editText.setSelection(editableText.length());
                    editText.requestFocus();
                    headerSubTextItem.setVisibility(View.GONE);
                } else {
                    hideKeyboard();
                    isResize[0] = false;
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    textInputLayout.setVisibility(View.GONE);
                    headerSubTextItem.setText(MessageFormat.format(editableText, element.getTextParams()));
                    headerSubTextItem.setVisibility(View.VISIBLE);
                    if (!editableText.equals(element.getSubText())) {
                        onEditTextChangeValueListener.onNewValueSet(editableText);
                    }
                }
            });
            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            headerView.setBackgroundResource(outValue.resourceId);
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addSeekBarItem(SeekBarElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            final boolean[] isResize = {false};
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.seekbar_view, null);
            RelativeLayout headerView = view.findViewById(R.id.head_item);
            ImageView headerImageLeftSide = headerView.findViewById(R.id.image_left);
            ImageView headerImageRightSide = headerView.findViewById(R.id.image_right);
            MaterialTextView headerTextItem = headerView.findViewById(R.id.item_text);
            MaterialTextView headerSubTextItem = headerView.findViewById(R.id.item_subtext);
            SeekBar seekBar = view.findViewById(R.id.seek_bar);
            seekBar.setMax(element.getMaxValue());
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                seekBar.setMin(element.getMinValue());
            }
            headerSubTextItem.setVisibility(View.VISIBLE);
            String vale = Objects.isNull(element.getSubText()) ? "" : element.getSubText();
            headerSubTextItem.setText(MessageFormat.format("{0}{1}", element.getMinValue(), vale));
            OnSeekBarChangeValueListener onSeekBarChangeValueListener = element.getOnSeekBarChangeValueListener();
            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int value = seekBar.getProgress() < element.getMinValue()
                            ? element.getMinValue()
                            : seekBar.getProgress();
                    headerSubTextItem.setText(MessageFormat.format("{0}{1}", value, element.getSubText()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {/* not implement*/}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int value = seekBar.getProgress() < element.getMinValue()
                            ? element.getMinValue()
                            : seekBar.getProgress();
                    onSeekBarChangeValueListener.onNewValueSet(value);
                }
            });

            if (element.getProgress() != 0) {
                seekBar.setProgress(element.getProgress());
            }
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .map(resId -> {
                        headerImageRightSide.setVisibility(View.VISIBLE);
                        return Glide.with(activity).load(resId).into(headerImageRightSide);
                    }).orElseGet(() -> Glide.with(activity).load(R.drawable.arrow_down).into(headerImageRightSide));

            Optional.of(element.getLeftSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(headerImageLeftSide);
                        headerImageLeftSide.setVisibility(View.VISIBLE);
                    });
            headerTextItem.setText(element.getTitle());
            headerView.setOnClickListener(v -> {
                if (!isResize[0]) {
                    isResize[0] = true;
                    Glide.with(activity).load(R.drawable.arrow_up).into(headerImageRightSide);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    view.getChildAt(1).setVisibility(View.VISIBLE);
                } else {
                    isResize[0] = false;
                    Glide.with(activity).load(R.drawable.arrow_down).into(headerImageRightSide);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    view.getChildAt(1).setVisibility(View.GONE);
                }
            });

            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            headerView.setBackgroundResource(outValue.resourceId);
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addCheckBoxItem(CheckBoxElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.checkbox_extendable_item_view, null);
            RelativeLayout headerView = view.findViewById(R.id.head_item);
            MaterialCheckBox checkBox = headerView.findViewById(R.id.checkbox_item);
            ImageView headerImageRightSide = headerView.findViewById(R.id.image_right);
            MaterialTextView headerTextItem = headerView.findViewById(R.id.item_text);
            MaterialTextView headerSubTextItem = headerView.findViewById(R.id.item_subtext);
            checkBox.setOnCheckedChangeListener(element.getOnCheckedChangeListener());
            checkBox.setChecked(element.isChecked());
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(headerImageRightSide);
                        headerImageRightSide.setVisibility(View.VISIBLE);
                    });
            Optional.ofNullable(element.getSubText())
                    .filter(subText -> !subText.isEmpty())
                    .ifPresent(subText -> {
                        headerSubTextItem.setText(subText);
                        headerSubTextItem.setVisibility(View.VISIBLE);
                    });
            headerTextItem.setText(element.getTitle());
            Optional.ofNullable(element.getExtendView()).ifPresent(extendView -> {
                extendView.setVisibility(View.GONE);
                view.addView(extendView);
            });
            if (element.isChecked()) {
                for (int i = 1; i < view.getChildCount(); i++) {
                    view.getChildAt(i).setVisibility(View.VISIBLE);
                }
            }
            checkBox.setOnClickListener(v -> {
                TransitionManager.endTransitions(pageView.findViewById(R.id.page_provider));
                if (checkBox.isChecked() && Objects.nonNull(element.getExtendView())) {
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    for (int i = 1; i < view.getChildCount(); i++) {
                        view.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                } else {
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    for (int i = 1; i < view.getChildCount(); i++) {
                        View childAt = view.getChildAt(i);
                        TransitionManager.beginDelayedTransition((ViewGroup) childAt, new ChangeBounds());
                        childAt.setVisibility(View.GONE);
                    }
                }
            });
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addSingleRadioChoiceItem(SingleChoiceElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            final boolean[] isResize = {false};
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.radio_choice_view, null);
            RelativeLayout headerView = view.findViewById(R.id.head_item);
            ImageView headerImageLeftSide = headerView.findViewById(R.id.image_left);
            ImageView headerImageRightSide = headerView.findViewById(R.id.image_right);
            MaterialTextView headerTextItem = headerView.findViewById(R.id.item_text);
            MaterialTextView headerSubTextItem = headerView.findViewById(R.id.item_subtext);
            RadioGroup radioGroup = view.findViewById(R.id.radio_group);
            for (int i = 0; i < element.getElements().size(); i++) {
                RadioButton radioButton = (RadioButton) layoutInflater.inflate(R.layout.radio_button, null);
                radioButton.setText(element.getElements().get(i));
                radioButton.setOnClickListener(v -> {
                    headerSubTextItem.setText(radioButton.getText());
                    headerSubTextItem.setVisibility(View.VISIBLE);
                });
                radioGroup.addView(radioButton);
                if (element.getElements().get(i).equalsIgnoreCase(element.getSelectedValue())) {
                    radioButton.setChecked(true);
                    headerSubTextItem.setVisibility(View.VISIBLE);
                    headerSubTextItem.setText(element.getElements().get(i));
                }
            }
            radioGroup.setOnCheckedChangeListener(element.getOnCheckedChangeListener());
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .map(resId -> {
                        headerImageRightSide.setVisibility(View.VISIBLE);
                        return Glide.with(activity).load(resId).into(headerImageRightSide);
                    }).orElseGet(() -> Glide.with(activity).load(R.drawable.arrow_down).into(headerImageRightSide));

            Optional.of(element.getLeftSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(headerImageLeftSide);
                        headerImageLeftSide.setVisibility(View.VISIBLE);
                    });
            headerTextItem.setText(element.getTitle());
            headerView.setOnClickListener(v -> {
                if (!isResize[0]) {
                    isResize[0] = true;
                    Glide.with(activity).load(R.drawable.arrow_up).into(headerImageRightSide);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    for (int i = 1; i < view.getChildCount(); i++) {
                        view.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                } else {
                    isResize[0] = false;
                    Glide.with(activity).load(R.drawable.arrow_down).into(headerImageRightSide);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    for (int i = 1; i < view.getChildCount(); i++) {
                        View childAt = view.getChildAt(i);
                        TransitionManager.beginDelayedTransition((ViewGroup) childAt);
                        childAt.setVisibility(View.GONE);
                    }
                }
            });

            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            headerView.setBackgroundResource(outValue.resourceId);
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addCustomItem(View view) {
        return addCustomItem(view, 0);
    }

    public NordanSimplyPage addCustomItem(View customView, int gravity) {
        RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.custom_item_view, null);
        LinearLayout customLinear = view.findViewById(R.id.custom_linear);
        if (gravity != 0) {
            customLinear.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            view.setGravity(gravity);
        }
        customLinear.addView(customView);
        ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
        return this;
    }

    public NordanSimplyPage addMinimalItem(BaseElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.minimal_item_view, null);
            MaterialTextView textItem = view.findViewById(R.id.item_text);
            textItem.setText(element.getTitle());
            if (element.getGravity() != 0) {
                int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, activity.getResources().getDisplayMetrics());
                RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(MATCH_PARENT, px);
                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                view.setLayoutParams(viewParams);
                view.setGravity(element.getGravity());
                textItem.setLayoutParams(textParams);
            }
            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
        });
        return this;
    }

    public NordanSimplyPage addExtendableItem(ExtendableElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.extendable_item_view, null);
            RelativeLayout headerView = view.findViewById(R.id.head_item);
            MaterialTextView textItem = headerView.findViewById(R.id.item_text);
            MaterialTextView subTextItem = headerView.findViewById(R.id.item_subtext);
            ImageView leftSideImage = headerView.findViewById(R.id.image_left);
            ImageView rightSideImage = headerView.findViewById(R.id.image_right);
            textItem.setText(element.getTitle());
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(rightSideImage);
                        rightSideImage.setVisibility(View.VISIBLE);
                    });
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .map(resId -> {
                        leftSideImage.setVisibility(View.VISIBLE);
                        return Glide.with(activity).load(resId).into(leftSideImage);
                    }).orElseGet(() -> Glide.with(activity).load(R.drawable.arrow_down).into(leftSideImage));
            Optional.ofNullable(element.getSubText())
                    .filter(subText -> !subText.isEmpty())
                    .ifPresent(subText -> {
                        subTextItem.setText(subText);
                        subTextItem.setVisibility(View.VISIBLE);
                    });
            Optional.ofNullable(element.getExtendView()).ifPresent(extendView -> {
                extendView.setVisibility(View.GONE);
                view.addView(extendView);
            });
            element.getExtendView().setVisibility(View.GONE);
            headerView.setOnClickListener(v -> {
                TransitionManager.endTransitions(pageView.findViewById(R.id.page_provider));
                if (element.getExtendView().getVisibility() == View.GONE) {
                    element.getExtendView().setVisibility(View.VISIBLE);
                    Glide.with(activity).load(R.drawable.arrow_up).into(leftSideImage);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    for (int i = 1; i < view.getChildCount(); i++) {
                        view.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                } else {
                    element.getExtendView().setVisibility(View.GONE);
                    Glide.with(activity).load(R.drawable.arrow_down).into(leftSideImage);
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    for (int i = 1; i < view.getChildCount(); i++) {
                        View childAt = view.getChildAt(i);
                        childAt.setVisibility(View.GONE);
                    }
                }
            });
            if (element.getGravity() != 0) {
                int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, activity.getResources().getDisplayMetrics());
                RelativeLayout.LayoutParams viewParams = new RelativeLayout.LayoutParams(MATCH_PARENT, px);
                RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(WRAP_CONTENT,
                        WRAP_CONTENT);
                view.setLayoutParams(viewParams);
                view.setGravity(element.getGravity());
                textItem.setLayoutParams(textParams);
            }
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addSwitchItem(SwitchElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.switch_extendable_item_view, null);
            RelativeLayout headerView = view.findViewById(R.id.head_item);
            MaterialTextView textItem = headerView.findViewById(R.id.item_text);
            MaterialTextView subTextItem = headerView.findViewById(R.id.item_subtext);
            SwitchMaterial switchItem = headerView.findViewById(R.id.switch_item);
            switchItem.setOnCheckedChangeListener(element.getOnCheckedChangeListener());
            textItem.setText(element.getTitle());
            switchItem.setChecked(element.isChecked());
            Optional.ofNullable(element.getSubText())
                    .filter(subText -> !subText.isEmpty())
                    .ifPresent(subText -> {
                        subTextItem.setText(subText);
                        subTextItem.setVisibility(View.VISIBLE);
                    });
            Optional.ofNullable(element.getExtendView()).ifPresent(extendView -> {
                extendView.setVisibility(View.GONE);
                view.addView(extendView);
            });
            switchItem.setOnClickListener(v -> {
                TransitionManager.endTransitions(pageView.findViewById(R.id.page_provider));
                if (switchItem.isChecked() && Objects.nonNull(element.getExtendView())) {
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider), new ChangeBounds());
                    for (int i = 1; i < view.getChildCount(); i++) {
                        view.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                } else {
                    TransitionManager.beginDelayedTransition(pageView.findViewById(R.id.page_provider));
                    for (int i = 1; i < view.getChildCount(); i++) {
                        View childAt = view.getChildAt(i);
                        TransitionManager.beginDelayedTransition((ViewGroup) childAt, new ChangeBounds());
                        childAt.setVisibility(View.GONE);
                    }
                }
            });
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addItem(PageElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.standard_item_view, null);
            ImageView imageLeftSide = view.findViewById(R.id.image_left);
            ImageView imageRightSide = view.findViewById(R.id.image_right);
            MaterialTextView textItem = view.findViewById(R.id.item_text);
            MaterialTextView subTextItem = view.findViewById(R.id.item_subtext);
            TypedValue outValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            view.setBackgroundResource(outValue.resourceId);
            if (element.getOnClickListener() != null) {
                view.setOnClickListener(element.getOnClickListener());
            } else if (element.getIntent() != null) {
                view.setOnClickListener(click -> activity.startActivity(element.getIntent()));
            }
            Optional.of(element.getLeftSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(imageLeftSide);
                        imageLeftSide.setVisibility(View.VISIBLE);
                    });
            Optional.of(element.getRightSideIconDrawable())
                    .filter(value -> value != 0)
                    .ifPresent(resId -> {
                        Glide.with(activity).load(resId).into(imageRightSide);
                        imageRightSide.setVisibility(View.VISIBLE);
                    });
            textItem.setText(element.getTitle());
            Optional.ofNullable(element.getSubText())
                    .filter(subText -> !subText.isEmpty())
                    .ifPresent(subText -> {
                        subTextItem.setText(subText);
                        subTextItem.setVisibility(View.VISIBLE);
                    });
            if (element.getGravity() != 0) {
                int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, activity.getResources().getDisplayMetrics());
                view.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, px));
                view.setGravity(element.getGravity());
            }
            LinearLayout wrapper = pageView.findViewById(R.id.page_provider);
            wrapper.addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addAccountItem(AccountElement parElement) {
        Optional.ofNullable(parElement).ifPresent(element -> {
            RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.account_item_view, null);
            ImageView accountPhoto = view.findViewById(R.id.account_photo);
            MaterialTextView textAccountName = view.findViewById(R.id.account_name);
            MaterialTextView textAccountEmail = view.findViewById(R.id.account_email);
            Optional.ofNullable(element.getAccountPhotoUrl()).ifPresent(uri -> {
                Glide.with(activity).load(uri).into(accountPhoto);
                accountPhoto.setVisibility(View.VISIBLE);
            });
            textAccountName.setText(element.getAccountName());
            textAccountEmail.setText(element.getAccountEmail());
            ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
            _addSeparator();
        });
        return this;
    }

    public NordanSimplyPage addImageItem(int resourceId) {
        return addImageItem(resourceId, 200, 200);
    }

    public NordanSimplyPage addImageItem(int resourceId, int width, int height) {
        int pxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, activity.getResources().getDisplayMetrics());
        int pxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, activity.getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(pxWidth, pxHeight);
        RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.image_item_view, null);
        ImageView image = view.findViewById(R.id.image);
        Glide.with(activity).load(resourceId).into(image);
        image.setLayoutParams(imageParams);
        ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
        return this;
    }

    public NordanSimplyPage addDescriptionItem(int resourceId) {
        return addDescriptionItem(activity.getString(resourceId));
    }

    public NordanSimplyPage addDescriptionItem(String message) {
        RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.description_item_view, null);
        MaterialTextView textDescription = view.findViewById(R.id.item_description);
        textDescription.setText(message);
        if (textDescription.getParent() != null) {
            ((ViewGroup) textDescription.getParent()).removeView(textDescription);
        }
        ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(textDescription);
        return this;
    }

    public NordanSimplyPage addGroup(int leftSideImageResourceId, String name) {
        return addGroup(name, leftSideImageResourceId, 0);
    }

    public NordanSimplyPage addGroup(String name, int colorResourceId) {
        return addGroup(name, 0, colorResourceId);
    }

    public NordanSimplyPage addGroup(String name) {
        return addGroup(name, 0, 0);
    }

    public NordanSimplyPage addGroup(@NonNull String name, int leftSideImageResourceId, int colorResourceId) {
        RelativeLayout view = (RelativeLayout) layoutInflater.inflate(R.layout.group_idem, null);
        ImageView imageLeftSide = view.findViewById(R.id.image_group);
        MaterialTextView groupTitle = view.findViewById(R.id.group_title);
        if (leftSideImageResourceId != 0) {
            Glide.with(activity).load(leftSideImageResourceId).into(imageLeftSide);
        } else {
            imageLeftSide.setVisibility(View.INVISIBLE);
        }
        if (colorResourceId != 0) {
            groupTitle.setTextColor(activity.getColor(colorResourceId));
        }
        groupTitle.setText(name);
        ((LinearLayout) pageView.findViewById(R.id.page_provider)).addView(view);
        return this;
    }

    public NordanSimplyPage addSeparator() {
        int dimensionPixelSize = activity.getResources().getDimensionPixelSize(R.dimen.nordan_simply_page_separator_height);
        ((LinearLayout) pageView.findViewById(R.id.page_provider))
                .addView(getSeparator(), new LayoutParams(MATCH_PARENT, dimensionPixelSize));
        return this;
    }

    private NordanSimplyPage _addSeparator() {
        if (isHideSeparators) {
            addEmptyItem(5);
            return this;
        }
        int dimensionPixelSize = activity.getResources().getDimensionPixelSize(R.dimen.nordan_simply_page_separator_height);
        ((LinearLayout) pageView.findViewById(R.id.page_provider))
                .addView(getSeparator(), new LayoutParams(MATCH_PARENT, dimensionPixelSize));
        return this;
    }

    public NordanSimplyPage addFacebook(String id) {
        addItem(nordanSimplyPagePatterns.createFacebookElement(id, "Facebook"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addFacebook(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createFacebookElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGooglePlayDeveloperPage(String developerId) {
        addItem(nordanSimplyPagePatterns.createGooglePlayDeveloperPageElement(developerId, "Google Play"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGooglePlayDeveloperPage(String developerId, String tittle) {
        addItem(nordanSimplyPagePatterns.createGooglePlayDeveloperPageElement(developerId, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGooglePlayApplicationPage(String applicationid, String tittle) {
        addItem(nordanSimplyPagePatterns.createGooglePlayApplicationPageElement(applicationid, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGooglePlayApplicationPage(String applicationid, String tittle, int iconResource) {
        addItem(nordanSimplyPagePatterns.createGooglePlayApplicationPageElement(applicationid, tittle, iconResource));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGithub(String id) {
        addItem(nordanSimplyPagePatterns.createGithubElement(id, "GitHub"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addGithub(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createGithubElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addInstagram(String id) {
        addItem(nordanSimplyPagePatterns.createInstagramElement(id, "Instagram"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addInstagram(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createInstagramElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addEmail(String email, String title) {
        addItem(nordanSimplyPagePatterns.createEmailElement(email, title));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addEmail(String email) {
        return addEmail(email, activity.getString(R.string.email));
    }

    public NordanSimplyPage addPhone(String phoneNumber, String title) {
        addItem(nordanSimplyPagePatterns.createPhoneItem(phoneNumber, title));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addSms(String phoneNumber, String title, String messageText) {
        addItem(nordanSimplyPagePatterns.createMessageItem(phoneNumber, title, messageText));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addWebsite(String url, String title) {
        addItem(nordanSimplyPagePatterns.createWebsiteElement(url, title, R.drawable.world_icon));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addWebsite(String url, String title, int resWebIcon) {
        addItem(nordanSimplyPagePatterns.createWebsiteElement(url, title, resWebIcon));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addLinkedIn(String id) {
        addItem(nordanSimplyPagePatterns.createLinkedInElement(id, "Linkedin"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addLinkedIn(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createLinkedInElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addSkype(String id) {
        addItem(nordanSimplyPagePatterns.createSkypeElement(id, "Skype"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addSkype(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createSkypeElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addYoutube(String id) {
        addItem(nordanSimplyPagePatterns.createYoutubeElement(id, "YouTube"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addYoutube(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createYoutubeElement(id, tittle));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addTwitter(String id) {
        addItem(nordanSimplyPagePatterns.createTwitterElement(id, "Twitter"));
        _addSeparator();
        return this;
    }

    public NordanSimplyPage addTwitter(String id, String tittle) {
        addItem(nordanSimplyPagePatterns.createTwitterElement(id, tittle));
        _addSeparator();
        return this;
    }

    public View create() {
        return pageView;
    }

    private View getSeparator() {
        return activity.getLayoutInflater().inflate(R.layout.separator_item, null);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
