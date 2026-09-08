export function getStorage<T = string>(key: string): T | null {
  try {
    const value = localStorage.getItem(key);
    if (value === null) return null;
    return JSON.parse(value) as T;
  } catch {
    return localStorage.getItem(key) as unknown as T;
  }
}

export function setStorage(key: string, value: string | number | boolean | object): void {
  const serialized = typeof value === 'object' ? JSON.stringify(value) : String(value);
  localStorage.setItem(key, serialized);
}

export function removeStorage(key: string): void {
  localStorage.removeItem(key);
}

export function clearStorage(): void {
  localStorage.clear();
}
