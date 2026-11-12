import { ProjectDetail } from "@/components/projects";
export const metadata = { title: "Conheça a iniciativa" };
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return <ProjectDetail id={(await params).id} />;
}
