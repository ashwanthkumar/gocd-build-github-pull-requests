package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.utils.collections.Iterables;
import in.ashwanthkumar.utils.lang.option.Option;
import org.eclipse.jgit.lib.Ref;

import java.util.ArrayList;
import java.util.List;

public class MergeRefs {
    private List<Ref> mergeRefs = new ArrayList<Ref>();

    public MergeRefs(List<Ref> mergeRefs) {
        this.mergeRefs = mergeRefs;
    }

    public boolean isEmpty() {
        return mergeRefs.isEmpty();
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public Ref head() {
        return Iterables.head(mergeRefs);
    }

    public Option<Ref> headOption() {
        return Iterables.headOption(mergeRefs);
    }
}
