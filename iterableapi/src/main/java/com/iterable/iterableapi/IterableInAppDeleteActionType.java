package com.iterable.iterableapi;

public enum IterableInAppDeleteActionType {

    INBOX_SWIPE {
        @Override
        public String toString() {
            return "inbox-swipe";
        }
    },

    DELETE_BUTTON {
        @Override
        public String toString() {
            return "delete-button";
        }
    },

    UNKNOWN {
        @Override
        public String toString() {
            return "unknown";
        }
    }



}
