import Table from "@/components/coverage/Table";

export default function CoveragePage() {
  return (
    <>
      <div className="relative min-h-screen overflow-hidden bg-white">
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#00000008_1px,transparent_1px),linear-gradient(to_bottom,#00000008_1px,transparent_1px)] bg-size-[4rem_4rem]" />
        <div className="relative mx-auto max-w-7xl px-6 lg:px-8 pt-20 pb-16">
          <Table />
        </div>
      </div>
    </>
  );
}
