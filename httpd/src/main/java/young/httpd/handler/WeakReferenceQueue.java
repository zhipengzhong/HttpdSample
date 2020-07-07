package young.httpd.handler;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;

public class WeakReferenceQueue<T> {

    private final ReferenceQueue mGarbage = new ReferenceQueue();
    private Object mStrongReference = new Object();
    private ListEntry mHead = new ListEntry(mStrongReference, mGarbage);
    private int mSize = 0;

    public void add(T obj) {
        cleanup();
        mSize++;
        new ListEntry(obj, mGarbage).insert(mHead.mPrev);
    }

    public void remove(T obj) {
        cleanup();
        ListEntry entry = mHead.mNext;
        while (entry != mHead) {
            Object other = entry.get();
            if (other == obj) {
                mSize--;
                entry.remove();
                return;
            }
            entry = entry.mNext;
        }
    }


    public Iterator<? extends T> iterator() {
        return new Iterator() {
            private ListEntry index = mHead;
            private Object next = null;

            public boolean hasNext() {
                next = null;
                while (next == null) {
                    ListEntry nextIndex = index.mPrev;
                    if (nextIndex == mHead) {
                        break;
                    }
                    next = nextIndex.get();
                    if (next == null) {
                        mSize--;
                        nextIndex.remove();
                    }
                }

                return next != null;
            }

            public T next() {
                hasNext();
                index = index.mPrev;
                return (T) next;
            }

            public void remove() {
                if (index != mHead) {
                    ListEntry nextIndex = index.mNext;
                    mSize--;
                    index.remove();
                    index = nextIndex;
                }
            }
        };
    }

    private void cleanup() {
        ListEntry entry;
        while ((entry = (ListEntry) mGarbage.poll()) != null) {
            mSize--;
            entry.remove();
        }
    }

    private static class ListEntry extends WeakReference {

        ListEntry mPrev, mNext;

        public ListEntry(Object o, ReferenceQueue queue) {
            super(o, queue);
            mPrev = this;
            mNext = this;
        }

        public void insert(ListEntry where) {
            mPrev = where;
            mNext = where.mNext;
            where.mNext = this;
            mNext.mPrev = this;
        }

        public void remove() {
            mPrev.mNext = mNext;
            mNext.mPrev = mPrev;
            mNext = this;
            mPrev = this;
        }
    }
}
