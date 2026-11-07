'use client';

import React, { useMemo } from 'react';
import ReactFlow, { Node, Edge, Background, Controls } from 'reactflow';
import 'reactflow/dist/style.css';
import { Solicitud } from '@/lib/types';
import { MapPin } from 'lucide-react';

interface Props {
  solicitudes: Solicitud[];
}

const CustomNode = React.memo(({ data }: any) => {
  return (
    <div className="flex flex-col items-center">
      <div className="mb-1 rounded-pill bg-white px-2 py-1 text-[12px] font-semibold shadow-md whitespace-nowrap text-black border border-gray-200">
        {data.label}
      </div>
      <MapPin className="text-blue-600 fill-white drop-shadow-md" size={36} />
    </div>
  );
});
CustomNode.displayName = 'CustomNode';

const nodeTypes = {
  customMapPin: CustomNode,
};

export default function MapReactFlow({ solicitudes }: Props) {
  const { nodes, edges } = useMemo(() => {
    // Filtramos solo las solicitudes que tienen latitud y longitud válidas
    const validSolicitudes = solicitudes.filter(
      (s) => s.latitud !== undefined && s.longitud !== undefined
    );
    
    const mappedNodes: Node[] = validSolicitudes.map((s) => {
      return {
        id: String(s.id),
        type: 'customMapPin',
        // Invertimos la latitud para que coincida con el eje Y del canvas (que va hacia abajo)
        // Multiplicamos por un factor amplio para separar los nodos visualmente en el plano infinito
        position: { x: s.longitud! * 10000, y: s.latitud! * -10000 },
        data: { 
          label: `#${s.id} - ${s.estado.toUpperCase()}`,
          fullData: s
        },
      };
    });

    const mappedEdges: Edge[] = [];
    // Conectamos los puntos simulando una "Ruta" de atención para el técnico
    for (let i = 0; i < mappedNodes.length - 1; i++) {
      mappedEdges.push({
        id: `e-${mappedNodes[i].id}-${mappedNodes[i+1].id}`,
        source: mappedNodes[i].id,
        target: mappedNodes[i+1].id,
        animated: true,
        style: { stroke: '#3b82f6', strokeWidth: 3, strokeDasharray: '5,5' },
      });
    }

    return { nodes: mappedNodes, edges: mappedEdges };
  }, [solicitudes]);

  if (nodes.length === 0) {
    return (
      <div className="flex h-[300px] items-center justify-center bg-gray-50 rounded-xl border border-gray-200">
        <p className="text-gray-500 font-medium">No hay coordenadas válidas para mostrar en el mapa</p>
      </div>
    );
  }

  return (
    <div className="w-full h-[400px] rounded-xl overflow-hidden border border-gray-200 shadow-sm relative bg-blue-50/30">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.8 }}
        minZoom={0.05}
        maxZoom={2}
        proOptions={{ hideAttribution: true }}
      >
        <Background gap={20} size={1} color="#cbd5e1" />
        <Controls showInteractive={false} />
      </ReactFlow>
    </div>
  );
}
