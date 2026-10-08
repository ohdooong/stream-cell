export function isAdminOnlyView(view: string): boolean {
  return view === 'topic-admin' || view === 'permissions';
}
