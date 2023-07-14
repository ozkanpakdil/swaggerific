package com.mascix.swaggerific.ui.textfx;

import javafx.application.Platform;

import java.util.*;

public class BracketHighlighter {

    private final CustomCodeArea codeArea;

    // the list of highlighted bracket pairs
    private final List<BracketPair> bracketPairs = new ArrayList<>();

    // constants that don't need to be created every time
    private final List<String> LOOP_STYLE = Collections.singletonList( "loop" );
    private final List<String> MATCH_STYLE = Arrays.asList( "match", "loop" );

    /**
     * Parameterized constructor
     * @param codeArea the code area
     */
    public BracketHighlighter(CustomCodeArea codeArea) {
        this.codeArea = codeArea;
        this.codeArea.addTextInsertionListener((start, end, text) -> clearBracket());
        //this.codeArea.textProperty().addListener((obs, oldVal, newVal) -> initializeBrackets(newVal)); // Let's not process all the text every time ;-)
        this.codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> Platform.runLater( () -> highlightBracket(newVal) ));
    }

    /**
     * Highlight the matching bracket at new caret position
     * @param newVal the new caret position
     */
    private void highlightBracket(int newVal) {

        // first clear existing bracket highlights
        this.clearBracket();

        // detect caret position both before and after bracket
        String prevChar = (newVal > 0) ? codeArea.getText(newVal - 1, newVal) : "";
        if (prevChar.equals("[") || prevChar.equals("]")) --newVal;

        // get other half of matching bracket
        Integer other = getMatchingBracket( newVal );

        if (other != null) {
            // other half exists
            BracketPair pair = new BracketPair(newVal, other);

            // highlight start and end
            styleBrackets( pair, MATCH_STYLE );

            // add bracket pair to list
            this.bracketPairs.add(pair);
        }
    }

    /**
     * Find the matching bracket location.
     * @param index to start searching from
     * @return null or position of matching bracket
     */
    private Integer getMatchingBracket( int index )
    {
        if ( index == codeArea.getLength() ) return null;

        char initialBracket = codeArea.getText( index, index+1 ).charAt(0);
        String BRACKET_PAIRS = "(){}[]<>";
        int bracketTypePosition = BRACKET_PAIRS.indexOf( initialBracket ); // "(){}[]<>"
        if ( bracketTypePosition < 0 ) return null;

        // even numbered bracketTypePositions are opening brackets, and odd positions are closing
        // if even (opening bracket) then step forwards, otherwise step backwards
        int stepDirection = ( bracketTypePosition % 2 == 0 ) ? +1 : -1;

        // the matching bracket to look for, the opposite of initialBracket
        char match = BRACKET_PAIRS.charAt( bracketTypePosition + stepDirection );

        index += stepDirection;
        int bracketCount = 1;

        while ( index > -1 && index < codeArea.getLength() ) {
            char code = codeArea.getText( index, index+1 ).charAt(0);
            if ( code == initialBracket ) bracketCount++;
            else if ( code == match ) bracketCount--;
            if ( bracketCount == 0 ) return index;
            else index += stepDirection;
        }

        return null;
    }

    /**
     * Highlight the matching bracket at current caret position
     */
    public void highlightBracket() {
        this.highlightBracket(codeArea.getCaretPosition());
    }

    /**
     * Clear the existing highlighted bracket styles
     */
    public void clearBracket() {

        Iterator<BracketPair> iterator = this.bracketPairs.iterator();

        while ( iterator.hasNext() )
        {
            // clear next bracket pair
            styleBrackets( iterator.next(), LOOP_STYLE );

            // remove bracket pair from list
            iterator.remove();
        }

    }

    private void styleBrackets( BracketPair pair, List<String> styles )
    {
        styleBracket( pair.start, styles );
        styleBracket( pair.end, styles );
    }

    private void styleBracket( int pos, List<String> styles )
    {
        if ( pos < codeArea.getLength() ) {
            String text = codeArea.getText( pos, pos + 1 );
            if ( text.equals("[") || text.equals("]") ) {
                codeArea.setStyle( pos, pos + 1, styles );
            }
        }
    }

    /**
     * Class representing a pair of matching bracket indices
     */
    static class BracketPair {

        private final int start;
        private final int end;

        public BracketPair(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        @Override
        public String toString() {
            return "BracketPair{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }

    }

}