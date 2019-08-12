package com.morocco.hamssa.utils;

import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.morocco.hamssa.data.Database;

import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by hmontaner on 15/09/15.
 */
public class ClickableTextViewHelper {
    public interface OnWordClickedListener{
        public void onMessageReferenceClicked(Long messageOrdinal);
        public void onUserReferenceClicked(String userName);
    }
    static public Pattern patternMessageReference = Pattern.compile("[0-9]+");
    static public Pattern patternUserReference = Pattern.compile("[a-zA-Z0-9_ñÑçÇáéíóúÁÉÍÓÚàÀèÈòÒüÜ]+");
    static public void initClickableTextView(TextView definitionView, Spanned definition, final OnWordClickedListener listener) {
        //definition = definition.trim();
        definitionView.setMovementMethod(LinkMovementMethod.getInstance());
        definitionView.setText(definition, TextView.BufferType.SPANNABLE);
        Spannable spans = (Spannable) definitionView.getText();
        String plainText = spans.toString();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.US);
        iterator.setText(plainText);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            final String possibleWord = plainText.substring(start, end);
            int match = 0;
            if(patternMessageReference.matcher(possibleWord).matches()) {
                Long messageReference = Long.parseLong(possibleWord);
                // Skip the beginning of the text "#ordinal_of_this_message blah blah blah"
                if (start > 1 && plainText.charAt(start - 1) == '#') {
                    match = 1;
                }
            }
          /* if(match == 0) {
                if(patternUserReference.matcher(possibleWord).matches()) {
                    if(plainText.charAt(start - 1) == '@') {
                        match = 2;
                    }
                }
            }*/
            final int matchFinal = match;

           /* if(match > 0) {
                ClickableSpan clickSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        //Toast.makeText(widget.getContext(), possibleReference, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            if(matchFinal == 1) {
                                try {
                                    listener.onMessageReferenceClicked(Long.parseLong(possibleWord));
                                }catch(NumberFormatException e){}
                            }else if(matchFinal == 2){
                                listener.onUserReferenceClicked(possibleWord);
                            }
                        }
                    }
                    public void updateDrawState(TextPaint ds) {
                        ds.setColor(ds.linkColor);
                    }
                };
                spans.setSpan(clickSpan, start - 1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }*/
        }
    }
}
