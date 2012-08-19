/*
 * The FML Forge Mod Loader suite. Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cpw.mods.fml.common.toposort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.toposort.TopologicalSort.DirectedGraph;
import cpw.mods.fml.common.versioning.ArtifactVersion;

/**
 * @author cpw
 *
 */
public class ModSorter
{
    private DirectedGraph<ModContainer> modGraph;

    private ModContainer beforeAll = new DummyModContainer();
    private ModContainer afterAll = new DummyModContainer();
    private ModContainer before = new DummyModContainer();
    private ModContainer after = new DummyModContainer();

    private List<ModContainer> immutableMods;

    public ModSorter(List<ModContainer> modList, Map<String, ModContainer> nameLookup)
    {
        buildGraph(modList, nameLookup);
    }

    private void buildGraph(List<ModContainer> modList, Map<String, ModContainer> nameLookup)
    {
        modGraph = new DirectedGraph<ModContainer>();
        immutableMods = Lists.newArrayList();
        modGraph.addNode(beforeAll);
        modGraph.addNode(before);
        modGraph.addNode(afterAll);
        modGraph.addNode(after);
        modGraph.addEdge(before, after);
        modGraph.addEdge(beforeAll, before);
        modGraph.addEdge(after, afterAll);

        for (ModContainer mod : modList)
        {
            if (!mod.isImmutable())
            {
                modGraph.addNode(mod);
            }
            else
            {
                immutableMods.add(mod);
            }
        }

        for (ModContainer mod : modList)
        {
            if (mod.isImmutable())
                continue;
            boolean preDepAdded = false;
            boolean postDepAdded = false;

            for (ArtifactVersion dep : mod.getDependencies())
            {
                preDepAdded = true;

                if (dep.getLabel().equals("*"))
                {
                    // We are "after" everything
                    modGraph.addEdge(mod, afterAll);
                    modGraph.addEdge(after, mod);
                    postDepAdded = true;
                }
                else
                {
                    modGraph.addEdge(before, mod);
                    if (nameLookup.containsKey(dep.getLabel()))
                    {
                        modGraph.addEdge(nameLookup.get(dep.getLabel()), mod);
                    }
                }
            }

            for (ArtifactVersion dep : mod.getDependants())
            {
                postDepAdded = true;

                if (dep.getLabel().equals("*"))
                {
                    // We are "before" everything
                    modGraph.addEdge(beforeAll, mod);
                    modGraph.addEdge(mod, before);
                    preDepAdded = true;
                }
                else
                {
                    modGraph.addEdge(mod, after);
                    if (nameLookup.containsKey(dep.getLabel()))
                    {
                        modGraph.addEdge(mod, nameLookup.get(dep.getLabel()));
                    }
                }
            }

            if (!preDepAdded)
            {
                modGraph.addEdge(before, mod);
            }

            if (!postDepAdded)
            {
                modGraph.addEdge(mod, after);
            }
        }
    }

    public List<ModContainer> sort()
    {
        List<ModContainer> sortedList = TopologicalSort.topologicalSort(modGraph);
        sortedList.removeAll(Arrays.asList(new ModContainer[] {beforeAll, before, after, afterAll}));
        immutableMods.addAll(sortedList);
        return immutableMods;
    }
}
