package com.mascix.swaggerific.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.ui.component.TreeItemOperationLeaf;
import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class TreeItemSerialisationWrapper<T extends Serializable> implements Serializable {
    private transient TreeItem<T> item;
    ObjectMapper mapper = new ObjectMapper();

    public TreeItemSerialisationWrapper(TreeItem<T> item) {
        if (item == null) {
            throw new IllegalArgumentException();
        }
        this.item = item;
    }

    /**
     * Custom way of writing the TreeItem structure
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
        Stack<Object> stack = new Stack<>();
        stack.push(item);

        out.defaultWriteObject();
        do {
            TreeItem current = (TreeItem) stack.pop();
            int size = current.getChildren().size();
            out.writeInt(size);

            // write all the data that needs to be restored here
            out.writeObject(current.getValue());
            if (current instanceof TreeItemOperationLeaf) {
                TreeItemOperationLeaf x = (TreeItemOperationLeaf) current;
                out.writeObject(x.getQueryItems());
                out.writeObject(mapper.writeValueAsString(x.getMethodParameters()));
                out.writeObject(x.getUri());
            } else {
                out.writeObject(List.of(""));
                out.writeObject(mapper.writeValueAsString(new ArrayList<Parameter>()));
                out.writeObject("");
            }

            // "schedule" serialisation of children.
            // the first one is inserted last, since the top one from the stack is
            // retrieved first
            for (int i = size - 1; i >= 0; --i) {
                stack.push(current.getChildren().get(i));
            }
        } while (!stack.isEmpty());
    }

    /**
     * happens before readResolve; recreates the TreeItem structure
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        class Container {
            int count;
            final TreeItem<T> item;
            final List<String> items;
            final List<Parameter> parameters;
            final String uri;

            Container(ObjectInputStream in) throws ClassNotFoundException, IOException {
                // read the data for a single TreeItem here
                this.count = in.readInt();
                this.item = new TreeItem<>((T) in.readObject());
                this.items = (List<String>) in.readObject();
                this.parameters = Arrays.asList(mapper.readValue((String) in.readObject(), Parameter[].class));
                this.uri = (String) in.readObject();
            }
        }
        in.defaultReadObject();
        Container root = new Container(in);
        this.item = root.item;

        if (root.count > 0) {
            Stack<Container> stack = new Stack<>();
            stack.push(root);
            do {
                Container current = stack.peek();
                --current.count;
                if (current.count <= 0) {
                    // we're done with this item
                    stack.pop();
                }

                Container newContainer = new Container(in);
                if (!newContainer.parameters.isEmpty()) {
                    TreeItemOperationLeaf tio = TreeItemOperationLeaf.builder().build();
                    tio.setValue(newContainer.item.getValue());
                    tio.setQueryItems(newContainer.items);
                    tio.setMethodParameters(newContainer.parameters);
                    tio.setUri(newContainer.uri);
                    current.item.getChildren().add(tio);
                } else {
                    current.item.getChildren().add(newContainer.item);
                }
                if (newContainer.count > 0) {
                    // schedule reading children of non-leaf
                    stack.push(newContainer);
                }

            } while (!stack.isEmpty());
        }
    }

    /**
     * We're not actually interested in this object but the treeitem
     *
     * @return the treeitem
     * @throws ObjectStreamException
     */
    private Object readResolve() throws ObjectStreamException {
        return item;
    }

}